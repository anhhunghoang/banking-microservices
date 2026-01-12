package com.banking.account.event;

import com.banking.common.event.AccountCreated;
import com.banking.common.event.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class AccountEventProducerImpl implements AccountEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "accounts.events";

    @Override
    public void sendAccountCreated(AccountCreated payload) {
        BaseEvent<AccountCreated> event = BaseEvent.<AccountCreated>builder()
                .eventId(UUID.randomUUID())
                .eventType("AccountCreated")
                .eventVersion(1)
                .payload(payload)
                .timestamp(java.time.LocalDateTime.now())
                .correlationId(UUID.randomUUID())
                .build();

        kafkaTemplate.send(TOPIC, payload.getAccountId().toString(), event);
        log.info("Sent AccountCreated event for account: {}", payload.getAccountId());
    }
}
