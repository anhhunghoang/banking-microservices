package com.banking.account.event;

import com.banking.account.model.OutboxEvent;
import com.banking.account.repository.OutboxRepository;
import com.banking.common.constant.AggregateTypes;
import com.banking.common.constant.EventTypes;
import com.banking.common.event.*;
import com.banking.common.tracing.TracingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class AccountEventProducerImpl implements AccountEventProducer {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final TracingService tracingService;

    @Override
    public void sendAccountCreated(AccountCreated payload) {
        saveEvent(EventTypes.ACCOUNT_CREATED, payload.getAccountId(), payload, null);
    }

    @Override
    public void sendMoneyCredited(MoneyCredited payload, UUID transactionId) {
        saveEvent(EventTypes.MONEY_CREDITED, payload.getAccountId(), payload, transactionId);
    }

    @Override
    public void sendMoneyDebited(MoneyDebited payload, UUID transactionId) {
        saveEvent(EventTypes.MONEY_DEBITED, payload.getAccountId(), payload, transactionId);
    }

    @Override
    public void sendMoneyReserved(MoneyReserved payload, UUID transactionId) {
        saveEvent(EventTypes.MONEY_RESERVED, payload.getAccountId(), payload, transactionId);
    }

    @Override
    public void sendReservationFailed(ReservationFailed payload, UUID transactionId) {
        saveEvent(EventTypes.RESERVATION_FAILED, payload.getAccountId(), payload, transactionId);
    }

    @Override
    public void sendRefundCompleted(RefundCompleted payload, UUID transactionId) {
        saveEvent(EventTypes.REFUND_COMPLETED, payload.getAccountId(), payload, transactionId);
    }

    private void saveEvent(String eventType, UUID aggregateId, Object payload, UUID transactionId) {
        try {
            BaseEvent<Object> event = BaseEvent.<Object>builder()
                    .eventId(UUID.randomUUID())
                    .eventType(eventType)
                    .eventVersion(1)
                    .aggregateType(AggregateTypes.ACCOUNT)
                    .aggregateId(aggregateId)
                    .transactionId(transactionId)
                    .traceId(tracingService.getCurrentTraceId())
                    .payload(payload)
                    .timestamp(LocalDateTime.now())
                    .correlationId(UUID.randomUUID())
                    .build();

            String payloadJson = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(AggregateTypes.ACCOUNT)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .build();

            outboxRepository.save(outboxEvent);
            log.info("Saved {} event to outbox for account: {}", eventType, aggregateId);
        } catch (JsonProcessingException e) {
            log.error("Error serializing outbox event for account: {}", aggregateId, e);
            throw new RuntimeException("Error serializing outbox event", e);
        }
    }
}
