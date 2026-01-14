package com.banking.account.event;

import com.banking.account.model.ProcessedEvent;
import com.banking.account.repository.ProcessedEventRepository;
import com.banking.account.service.AccountService;
import com.banking.common.event.BaseEvent;
import com.banking.common.event.DepositRequested;
import com.banking.common.event.RefundRequested;
import com.banking.common.event.TransferRequested;
import com.banking.common.event.WithdrawRequested;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class AccountEventListener {

    private final AccountService accountService;
    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(topics = "transactions.commands", groupId = "account-service-group")
    @Transactional
    public void handleTransactionCommands(String message) {
        log.info("Received transaction command message: {}", message);

        try {
            TypeReference<BaseEvent<Object>> typeRef = new TypeReference<>() {
            };
            BaseEvent<Object> event = objectMapper.readValue(message, typeRef);

            UUID eventId = event.getEventId();
            if (eventId != null && processedEventRepository.existsById(eventId)) {
                log.info("Event {} already processed. Skipping.", eventId);
                return;
            }

            log.info("Processing event type: {} for transaction: {}",
                    event.getEventType(), event.getTransactionId());

            switch (event.getEventType()) {
                case "DepositRequested" -> {
                    String json = objectMapper.writeValueAsString(event.getPayload());
                    DepositRequested payload = objectMapper.readValue(json, DepositRequested.class);
                    accountService.deposit(payload.getAccountId(), payload.getAmount(), event.getTransactionId());
                }
                case "WithdrawRequested" -> {
                    String json = objectMapper.writeValueAsString(event.getPayload());
                    WithdrawRequested payload = objectMapper.readValue(json, WithdrawRequested.class);
                    accountService.withdraw(payload.getAccountId(), payload.getAmount(), event.getTransactionId());
                }
                case "TransferRequested" -> {
                    String json = objectMapper.writeValueAsString(event.getPayload());
                    TransferRequested payload = objectMapper.readValue(json, TransferRequested.class);
                    accountService.reserveMoney(payload.getFromAccountId(), payload.getAmount(),
                            event.getTransactionId());
                }
                case "RefundRequested" -> {
                    String json = objectMapper.writeValueAsString(event.getPayload());
                    RefundRequested payload = objectMapper.readValue(json, RefundRequested.class);
                    accountService.refund(payload.getAccountId(), payload.getAmount(), event.getTransactionId());
                }
                default -> log.warn("Unhandled event type: {}", event.getEventType());
            }

            if (eventId != null) {
                processedEventRepository.save(new ProcessedEvent(eventId, null));
            }

        } catch (Exception e) {
            log.error("Error processing transaction command. Message: {}", message, e);
            // In a real system, we might NOT throw here if we want to move to DLQ via
            // manual handling
            // but for training, let's keep it simple.
        }
    }
}
