package com.banking.account.event;

import com.banking.common.event.AccountCreated;
import com.banking.common.event.BaseEvent;
import com.banking.common.event.MoneyCredited;
import com.banking.common.event.MoneyDebited;
import com.banking.common.event.MoneyReserved;
import com.banking.common.event.ReservationFailed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class AccountEventProducerImpl implements AccountEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "accounts.events";

    @Override
    public void sendAccountCreated(AccountCreated payload) {
        sendEvent("AccountCreated", payload.getAccountId(), payload, null);
    }

    @Override
    public void sendMoneyCredited(MoneyCredited payload, UUID transactionId) {
        sendEvent("MoneyCredited", payload.getAccountId(), payload, transactionId);
    }

    @Override
    public void sendMoneyDebited(MoneyDebited payload, UUID transactionId) {
        sendEvent("MoneyDebited", payload.getAccountId(), payload, transactionId);
    }

    @Override
    public void sendMoneyReserved(MoneyReserved payload, UUID transactionId) {
        sendEvent("MoneyReserved", payload.getAccountId(), payload, transactionId);
    }

    @Override
    public void sendReservationFailed(ReservationFailed payload, UUID transactionId) {
        sendEvent("ReservationFailed", payload.getAccountId(), payload, transactionId);
    }

    private void sendEvent(String eventType, UUID aggregateId, Object payload, UUID transactionId) {
        BaseEvent<Object> event = BaseEvent.<Object>builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .eventVersion(1)
                .aggregateType("Account")
                .aggregateId(aggregateId)
                .transactionId(transactionId)
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .correlationId(UUID.randomUUID())
                .build();

        kafkaTemplate.send(TOPIC, aggregateId.toString(), event);
        log.info("Sent {} event for account: {}", eventType, aggregateId);
    }
}
