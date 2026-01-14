package com.banking.transaction.event;

import com.banking.common.constant.EventTypes;
import com.banking.common.event.BaseEvent;
import com.banking.transaction.model.Transaction;
import com.banking.transaction.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionEventListenerTest {

    @Mock
    private TransactionRepository transactionRepository;

    private ObjectMapper objectMapper;

    @InjectMocks
    private TransactionEventListener transactionEventListener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        transactionEventListener = new TransactionEventListener(transactionRepository, objectMapper);
    }

    @Test
    void shouldUpdateTransactionToCompleted_WhenMoneyCreditedReceived() throws Exception {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .status(Transaction.TransactionStatus.PENDING)
                .build();

        BaseEvent<Object> event = BaseEvent.builder()
                .transactionId(transactionId)
                .eventType(EventTypes.MONEY_CREDITED)
                .build();

        String message = objectMapper.writeValueAsString(event);
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        // Act
        transactionEventListener.handleAccountEvents(message);

        // Assert
        verify(transactionRepository).save(argThat(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED));
    }

    @Test
    void shouldUpdateTransactionToFailed_WhenReservationFailedReceived() throws Exception {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .status(Transaction.TransactionStatus.PENDING)
                .build();

        BaseEvent<Object> event = BaseEvent.builder()
                .transactionId(transactionId)
                .eventType(EventTypes.RESERVATION_FAILED)
                .build();

        String message = objectMapper.writeValueAsString(event);
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        // Act
        transactionEventListener.handleAccountEvents(message);

        // Assert
        verify(transactionRepository).save(argThat(t -> t.getStatus() == Transaction.TransactionStatus.FAILED));
    }

    @Test
    void shouldUpdateTransactionToFailed_WhenRefundCompletedReceived() throws Exception {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .status(Transaction.TransactionStatus.PENDING)
                .build();

        BaseEvent<Object> event = BaseEvent.builder()
                .transactionId(transactionId)
                .eventType(EventTypes.REFUND_COMPLETED)
                .build();

        String message = objectMapper.writeValueAsString(event);
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        // Act
        transactionEventListener.handleAccountEvents(message);

        // Assert
        verify(transactionRepository).save(argThat(t -> t.getStatus() == Transaction.TransactionStatus.FAILED));
    }

    @Test
    void shouldHandleTransactionNotFound_Gracefully() throws Exception {
        // Arrange
        UUID transactionId = UUID.randomUUID();

        BaseEvent<Object> event = BaseEvent.builder()
                .transactionId(transactionId)
                .eventType(EventTypes.MONEY_CREDITED)
                .build();

        String message = objectMapper.writeValueAsString(event);
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        // Act
        transactionEventListener.handleAccountEvents(message);

        // Assert
        verify(transactionRepository, never()).save(any());
    }
}
