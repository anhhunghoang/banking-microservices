package com.banking.transaction.service;

import com.banking.transaction.dto.TransactionRequest;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.model.OutboxEvent;
import com.banking.transaction.model.Transaction;
import com.banking.transaction.repository.OutboxRepository;
import com.banking.transaction.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private TransactionRequest depositRequest;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        depositRequest = TransactionRequest.builder()
                .accountId(accountId)
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .build();
    }

    @Test
    void shouldCreateDepositTransaction_AndSaveOutboxEvent() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        Transaction savedTransaction = Transaction.builder()
                .id(transactionId)
                .accountId(accountId)
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .type(Transaction.TransactionType.DEPOSIT)
                .status(Transaction.TransactionStatus.PENDING)
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        // Act
        TransactionResponse response = transactionService.createDeposit(depositRequest);

        // Assert
        assertThat(response.getAccountId()).isEqualTo(accountId);
        assertThat(response.getType()).isEqualTo("DEPOSIT");
        assertThat(response.getStatus()).isEqualTo("PENDING");

        verify(transactionRepository).save(any(Transaction.class));
        verify(outboxRepository).save(any(OutboxEvent.class));
    }
}
