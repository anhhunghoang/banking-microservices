package com.banking.transaction.service;

import com.banking.common.constant.EventTypes;
import com.banking.transaction.model.OutboxEvent;
import com.banking.transaction.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutboxProcessor outboxProcessor;

    @Test
    void shouldProcessPendingEvents_AndSendToKafka() {
        // Arrange
        UUID aggregateId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateId(aggregateId)
                .eventType(EventTypes.DEPOSIT_REQUESTED)
                .payload("{\"test\":\"data\"}")
                .status(OutboxEvent.OutboxStatus.PENDING)
                .build();

        when(outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus.PENDING))
                .thenReturn(List.of(event));

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(new CompletableFuture<>());

        // Act
        outboxProcessor.processOutboxEvents();

        // Assert
        verify(kafkaTemplate).send(anyString(), eq(aggregateId.toString()), eq("{\"test\":\"data\"}"));
        verify(outboxRepository).save(argThat(e -> e.getStatus() == OutboxEvent.OutboxStatus.PROCESSED));
    }

    @Test
    void shouldMarkEventAsFailed_WhenKafkaSendFails() {
        // Arrange
        UUID aggregateId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateId(aggregateId)
                .eventType(EventTypes.DEPOSIT_REQUESTED)
                .payload("{\"test\":\"data\"}")
                .status(OutboxEvent.OutboxStatus.PENDING)
                .build();

        when(outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus.PENDING))
                .thenReturn(List.of(event));

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Kafka down"));

        // Act
        outboxProcessor.processOutboxEvents();

        // Assert
        verify(outboxRepository).save(argThat(e -> e.getStatus() == OutboxEvent.OutboxStatus.FAILED));
    }
}
