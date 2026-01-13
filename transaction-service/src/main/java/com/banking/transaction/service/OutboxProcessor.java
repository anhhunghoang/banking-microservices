package com.banking.transaction.service;

import com.banking.transaction.model.OutboxEvent;
import com.banking.transaction.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> events = outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus.PENDING);

        if (events.isEmpty()) {
            return;
        }

        log.info("Processing {} outbox events", events.size());

        for (OutboxEvent event : events) {
            try {
                String topic = determineTopic(event.getEventType());
                // The Aspect in common-lib will automatically catch this send,
                // see the trace_id in the JSON, and restore the Jaeger chain!
                kafkaTemplate.send(topic, event.getAggregateId().toString(), event.getPayload());

                event.setStatus(OutboxEvent.OutboxStatus.PROCESSED);
                event.setProcessedAt(Instant.now());
                outboxRepository.save(event);

                log.info("Successfully processed outbox event: {} to topic: {}", event.getId(), topic);
            } catch (Exception e) {
                log.error("Error processing outbox event: {}", event.getId(), e);
                event.setStatus(OutboxEvent.OutboxStatus.FAILED);
                outboxRepository.save(event);
            }
        }
    }

    private String determineTopic(String eventType) {
        return switch (eventType) {
            case "DepositRequested", "WithdrawRequested", "TransferRequested" -> "transactions.commands";
            case "TransactionCompleted", "TransactionFailed" -> "transactions.events";
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }
}
