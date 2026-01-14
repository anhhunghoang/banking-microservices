package com.banking.account.service;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.event.AccountEventProducer;
import com.banking.account.model.Account;
import com.banking.account.repository.AccountRepository;
import com.banking.common.event.AccountCreated;
import com.banking.common.event.MoneyCredited;
import com.banking.common.event.MoneyDebited;
import com.banking.common.event.MoneyReserved;
import com.banking.common.event.RefundCompleted;
import com.banking.common.event.ReservationFailed;
import com.banking.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private static final String INSUFFICIENT_FUNDS = "Insufficient funds";
    private static final String ERROR_CODE_INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS";
    private static final String ACCOUNT_FROZEN = "Account is frozen";
    private static final String ERROR_CODE_ACCOUNT_FROZEN = "ACCOUNT_FROZEN";

    private final AccountRepository accountRepository;
    private final AccountEventProducer eventProducer;

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Account account = Account.builder()
                .customerId(request.getCustomerId())
                .balance(request.getInitialBalance())
                .status(Account.AccountStatus.ACTIVE)
                .build();

        Account savedAccount = accountRepository.save(account);

        eventProducer.sendAccountCreated(AccountCreated.builder()
                .accountId(savedAccount.getId())
                .customerId(savedAccount.getCustomerId())
                .initialBalance(savedAccount.getBalance())
                .build());

        return mapToResponse(savedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID id) {
        return accountRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new BusinessException("Account not found", "ACCOUNT_NOT_FOUND"));
    }

    @Override
    @Transactional
    public void deposit(UUID id, BigDecimal amount, UUID transactionId) {
        processDeposit(id, amount, transactionId);
    }

    @Override
    @Transactional
    public void withdraw(UUID id, BigDecimal amount, UUID transactionId) {
        processWithdraw(id, amount, transactionId);
    }

    @Override
    @Transactional
    public void reserveMoney(UUID id, BigDecimal amount, UUID transactionId) {
        Account account = getAccountEntity(id);

        checkAccountStatus(account, transactionId);

        if (account.getBalance().compareTo(amount) < 0) {
            log.warn("Insufficient funds for account: {} in transaction: {}", id, transactionId);
            eventProducer.sendReservationFailed(ReservationFailed.builder()
                    .accountId(id)
                    .reason(INSUFFICIENT_FUNDS)
                    .build(), transactionId);
            throw new BusinessException(INSUFFICIENT_FUNDS, ERROR_CODE_INSUFFICIENT_FUNDS);
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        eventProducer.sendMoneyReserved(MoneyReserved.builder()
                .accountId(id)
                .amount(amount)
                .currency("USD")
                .build(), transactionId);
    }

    @Override
    @Transactional
    public void debitMoney(UUID id, BigDecimal amount, UUID transactionId) {
        processWithdraw(id, amount, transactionId);
    }

    @Override
    @Transactional
    public void creditMoney(UUID id, BigDecimal amount, UUID transactionId) {
        processDeposit(id, amount, transactionId);
    }

    @Override
    @Transactional
    public void refund(UUID id, BigDecimal amount, UUID transactionId) {
        Account account = getAccountEntity(id);
        checkAccountStatus(account, transactionId);

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        eventProducer.sendRefundCompleted(RefundCompleted.builder()
                .accountId(id)
                .amount(amount)
                .currency("USD")
                .build(), transactionId);

        log.info("Successfully refunded {} to account: {}", amount, id);
    }

    private void processDeposit(UUID id, BigDecimal amount, UUID transactionId) {
        Account account = getAccountEntity(id);

        checkAccountStatus(account, transactionId);

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        eventProducer.sendMoneyCredited(MoneyCredited.builder()
                .accountId(id)
                .amount(amount)
                .currency("USD")
                .build(), transactionId);

        log.info("Successfully deposited {} to account: {}", amount, id);
    }

    private void processWithdraw(UUID id, BigDecimal amount, UUID transactionId) {
        Account account = getAccountEntity(id);

        checkAccountStatus(account, transactionId);

        if (account.getBalance().compareTo(amount) < 0) {
            log.warn("Insufficient funds for account: {} in transaction: {}", id, transactionId);
            if (transactionId != null) {
                eventProducer.sendReservationFailed(ReservationFailed.builder()
                        .accountId(id)
                        .reason(INSUFFICIENT_FUNDS)
                        .build(), transactionId);
            }
            throw new BusinessException(INSUFFICIENT_FUNDS, ERROR_CODE_INSUFFICIENT_FUNDS);
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        if (transactionId != null) {
            eventProducer.sendMoneyDebited(MoneyDebited.builder()
                    .accountId(id)
                    .amount(amount)
                    .currency("USD")
                    .build(), transactionId);
        }

        log.info("Successfully withdrew {} from account: {}", amount, id);
    }

    private void checkAccountStatus(Account account, UUID transactionId) {
        if (account.getStatus() == Account.AccountStatus.FROZEN) {
            log.error("Attempted operation on frozen account: {} in transaction: {}", account.getId(), transactionId);
            // Even if frozen, we might want to emit a failure event if this is part of a
            // transaction
            if (transactionId != null) {
                eventProducer.sendReservationFailed(ReservationFailed.builder()
                        .accountId(account.getId())
                        .reason(ACCOUNT_FROZEN)
                        .build(), transactionId);
            }
            throw new BusinessException(ACCOUNT_FROZEN, ERROR_CODE_ACCOUNT_FROZEN);
        }
    }

    private Account getAccountEntity(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Account not found", "ACCOUNT_NOT_FOUND"));
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .customerId(account.getCustomerId())
                .balance(account.getBalance())
                .status(account.getStatus().name())
                .build();
    }
}
