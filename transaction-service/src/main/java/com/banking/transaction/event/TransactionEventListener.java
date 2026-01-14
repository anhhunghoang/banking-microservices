package com.banking.transaction.event;

import com.banking.common.constant.EventTypes;
import com.banking.common.constant.ServiceGroups;
import com.banking.common.constant.Topics;
import com.banking.common.event.BaseEvent;
import com.banking.transaction.model.Transaction;
import com.banking.transaction.repository.TransactionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventListener {

    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = Topics.ACCOUNTS_EVENTS, groupId = ServiceGroups.TRANSACTION_SERVICE_GROUP)
    @Transactional
    public void handleAccountEvents(String message) {
        log.info("Received account event message: {}", message);

        try {
            TypeReference<BaseEvent<Object>> typeRef = new TypeReference<>() {
            };
            BaseEvent<Object> event = objectMapper.readValue(message, typeRef);

            log.info("Processing event type: {} for transaction: {}",
                    event.getEventType(), event.getTransactionId());

            switch (event.getEventType()) {
                case EventTypes.MONEY_RESERVED -> handleMoneyReserved(event);
                case EventTypes.MONEY_CREDITED -> handleMoneyCredited(event);
                case EventTypes.MONEY_DEBITED -> handleMoneyDebited(event);
                case EventTypes.RESERVATION_FAILED -> handleReservationFailed(event);
                case EventTypes.REFUND_COMPLETED -> handleRefundCompleted(event);
                default -> log.warn("Unknown event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing account event. Message: {}", message, e);
        }
    }

    private void handleMoneyReserved(BaseEvent<?> event) {
        UUID transactionId = event.getTransactionId();
        log.info("Money reserved for transaction: {}", transactionId);
    }

    private void handleMoneyCredited(BaseEvent<?> event) {
        UUID transactionId = event.getTransactionId();
        log.info("Money credited for transaction: {}", transactionId);

        updateTransactionStatus(transactionId, Transaction.TransactionStatus.COMPLETED);
    }

    private void handleMoneyDebited(BaseEvent<?> event) {
        UUID transactionId = event.getTransactionId();
        log.info("Money debited for transaction: {}", transactionId);

        updateTransactionStatus(transactionId, Transaction.TransactionStatus.COMPLETED);
    }

    private void handleReservationFailed(BaseEvent<?> event) {
        UUID transactionId = event.getTransactionId();
        log.error("Reservation failed for transaction: {}", transactionId);

        updateTransactionStatus(transactionId, Transaction.TransactionStatus.FAILED);
    }

    private void handleRefundCompleted(BaseEvent<?> event) {
        UUID transactionId = event.getTransactionId();
        log.info("Refund completed for transaction: {}", transactionId);

        updateTransactionStatus(transactionId, Transaction.TransactionStatus.FAILED);
    }

    private void updateTransactionStatus(UUID transactionId, Transaction.TransactionStatus status) {
        transactionRepository.findById(transactionId).ifPresent(transaction -> {
            transaction.setStatus(status);
            transactionRepository.save(transaction);
            log.info("Transaction {} marked as {}", transactionId, status);
        });
    }
}
