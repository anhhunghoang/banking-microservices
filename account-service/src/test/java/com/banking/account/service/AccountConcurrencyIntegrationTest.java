package com.banking.account.service;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.model.Account;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.OutboxRepository;
import com.banking.account.repository.ProcessedEventRepository;
import com.banking.account.event.AccountEventListener;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class AccountConcurrencyIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        processedEventRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void testConcurrentDeposits() throws InterruptedException {
        // Arrange: Create an account
        CreateAccountRequest createRequest = CreateAccountRequest.builder()
                .customerId(UUID.randomUUID())
                .initialBalance(BigDecimal.ZERO)
                .build();
        AccountResponse accountResponse = accountService.createAccount(createRequest);
        UUID accountId = accountResponse.getId();

        int threadCount = 10;
        BigDecimal depositAmount = BigDecimal.valueOf(10.0);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Act: Perform 10 concurrent deposits
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    accountService.deposit(accountId, depositAmount, UUID.randomUUID());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("Deposit failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Assert
        Account finalAccount = accountRepository.findById(accountId).orElseThrow();

        System.out.println("Successful deposits: " + successCount.get());
        System.out.println("Failed deposits: " + failureCount.get());
        System.out.println("Final balance: " + finalAccount.getBalance());

        // With @Version (Optimistic Locking), we expect some failures unless we
        // implement retries.
        // But the balance must be EXACTLY (successCount * depositAmount)
        BigDecimal expectedBalance = depositAmount.multiply(BigDecimal.valueOf(successCount.get()));
        assertThat(finalAccount.getBalance().stripTrailingZeros())
                .isEqualTo(expectedBalance.stripTrailingZeros());
    }

    @Test
    @Transactional
    void testMultipleDepositsInSingleTransaction() {
        // Arrange
        CreateAccountRequest createRequest = CreateAccountRequest.builder()
                .customerId(UUID.randomUUID())
                .initialBalance(BigDecimal.ZERO)
                .build();
        AccountResponse accountResponse = accountService.createAccount(createRequest);
        UUID accountId = accountResponse.getId();
        UUID transactionId = UUID.randomUUID();

        // Act: Call deposit 3 times in the SAME transaction
        accountService.deposit(accountId, BigDecimal.valueOf(100.0), transactionId);
        accountService.deposit(accountId, BigDecimal.valueOf(50.0), transactionId);
        accountService.deposit(accountId, BigDecimal.valueOf(25.0), transactionId);

        // Assert
        Account finalAccount = accountRepository.findById(accountId).orElseThrow();

        // Balance should be exactly 100 + 50 + 25 = 175
        assertThat(finalAccount.getBalance().stripTrailingZeros())
                .isEqualTo(BigDecimal.valueOf(175.0).stripTrailingZeros());

        // We should have exactly 1 AccountCreated + 3 MoneyCredited = 4 outbox events
        long outboxCount = outboxRepository.count();
        assertThat(outboxCount).isEqualTo(4);

        System.out.println("Final balance in single transaction: " + finalAccount.getBalance());
        System.out.println("Total outbox events: " + outboxCount);
    }

    @Test
    void testFrozenAccountRejectsDeposits() {
        // Arrange
        CreateAccountRequest createRequest = CreateAccountRequest.builder()
                .customerId(UUID.randomUUID())
                .initialBalance(BigDecimal.valueOf(100.0))
                .build();
        AccountResponse accountResponse = accountService.createAccount(createRequest);
        UUID accountId = accountResponse.getId();

        // Freeze the account
        Account account = accountRepository.findById(accountId).orElseThrow();
        account.setStatus(Account.AccountStatus.FROZEN);
        accountRepository.save(account);

        // Act & Assert
        try {
            accountService.deposit(accountId, BigDecimal.valueOf(10.0), UUID.randomUUID());
        } catch (com.banking.common.exception.BusinessException e) {
            assertThat(e.getErrorCode()).isEqualTo("ACCOUNT_FROZEN");
        }

        // Verify balance didn't change
        Account finalAccount = accountRepository.findById(accountId).orElseThrow();
        assertThat(finalAccount.getBalance().stripTrailingZeros())
                .isEqualTo(BigDecimal.valueOf(100.0).stripTrailingZeros());
    }

    @Autowired
    private AccountEventListener accountEventListener;

    @Test
    void testEventIdempotency() throws Exception {
        // Arrange
        CreateAccountRequest createRequest = CreateAccountRequest.builder()
                .customerId(UUID.randomUUID())
                .initialBalance(BigDecimal.valueOf(100.0))
                .build();
        AccountResponse accountResponse = accountService.createAccount(createRequest);
        UUID accountId = accountResponse.getId();
        UUID eventId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        String message = String.format(
                "{\"event_id\":\"%s\",\"event_type\":\"DepositRequested\",\"transaction_id\":\"%s\",\"payload\":{\"account_id\":\"%s\",\"amount\":50.0}}",
                eventId, transactionId, accountId);

        // Act: Process same message twice
        accountEventListener.handleTransactionCommands(message);
        accountEventListener.handleTransactionCommands(message);

        // Assert
        Account finalAccount = accountRepository.findById(accountId).orElseThrow();

        // Balance should have increased ONLY ONCE (100 + 50 = 150)
        assertThat(finalAccount.getBalance().stripTrailingZeros())
                .isEqualTo(BigDecimal.valueOf(150.0).stripTrailingZeros());

        // ProcessedEvent should exist
        assertThat(processedEventRepository.existsById(eventId)).isTrue();
    }

    @Test
    void testRefundRequestedIncreasesBalance() throws Exception {
        // Arrange
        CreateAccountRequest createRequest = CreateAccountRequest.builder()
                .customerId(UUID.randomUUID())
                .initialBalance(BigDecimal.valueOf(100.0))
                .build();
        AccountResponse accountResponse = accountService.createAccount(createRequest);
        UUID accountId = accountResponse.getId();
        UUID eventId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        String message = String.format(
                "{\"event_id\":\"%s\",\"event_type\":\"RefundRequested\",\"transaction_id\":\"%s\",\"payload\":{\"account_id\":\"%s\",\"amount\":25.0}}",
                eventId, transactionId, accountId);

        // Act
        accountEventListener.handleTransactionCommands(message);

        // Assert
        Account finalAccount = accountRepository.findById(accountId).orElseThrow();

        // Balance should be 100 + 25 = 125
        assertThat(finalAccount.getBalance().stripTrailingZeros())
                .isEqualTo(BigDecimal.valueOf(125.0).stripTrailingZeros());

        // Check outbox for RefundCompleted
        long outboxCount = outboxRepository.findAll().stream()
                .filter(e -> e.getEventType().equals("RefundCompleted"))
                .count();
        assertThat(outboxCount).isEqualTo(1);
    }
}
