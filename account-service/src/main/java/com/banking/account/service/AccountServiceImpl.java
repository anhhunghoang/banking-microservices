package com.banking.account.service;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.event.AccountEventProducer;
import com.banking.account.model.Account;
import com.banking.account.repository.AccountRepository;
import com.banking.common.event.AccountCreated;
import com.banking.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

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
    public void deposit(UUID id, BigDecimal amount) {
        Account account = getAccountEntity(id);
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void withdraw(UUID id, BigDecimal amount) {
        Account account = getAccountEntity(id);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("Insufficient funds", "INSUFFICIENT_FUNDS");
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void reserveMoney(UUID id, BigDecimal amount, UUID transactionId) {
        // Simple implementation: subtract from balance for now
        // In a real outbox/saga, we might use a separate 'reserved_balance' field
        withdraw(id, amount);
    }

    @Override
    @Transactional
    public void debitMoney(UUID id, BigDecimal amount, UUID transactionId) {
        withdraw(id, amount);
    }

    @Override
    @Transactional
    public void creditMoney(UUID id, BigDecimal amount, UUID transactionId) {
        deposit(id, amount);
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
