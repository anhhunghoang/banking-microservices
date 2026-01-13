package com.banking.account.event;

import com.banking.account.service.AccountService;
import com.banking.common.event.BaseEvent;
import com.banking.common.event.DepositRequested;
import com.banking.common.event.TransferRequested;
import com.banking.common.event.WithdrawRequested;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AccountEventListener {

    private final AccountService accountService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "transactions.commands", groupId = "account-service-group")
    public void handleTransactionCommands(String message) {
        log.info("Received transaction command message: {}", message);

        try {
            // Deserialize into a generic map first to get the type, or directly into
            // BaseEvent
            // TypeReference is needed for generic types with Jackson
            TypeReference<BaseEvent<Object>> typeRef = new TypeReference<>() {
            };
            BaseEvent<Object> event = objectMapper.readValue(message, typeRef);

            log.info("Processing event type: {} for transaction: {}",
                    event.getEventType(), event.getTransactionId());

            switch (event.getEventType()) {
                case "DepositRequested" -> {
                    String json = objectMapper.writeValueAsString(event.getPayload());
                    DepositRequested payload = objectMapper.readValue(json, DepositRequested.class);
                    accountService.deposit(payload.getAccountId(), payload.getAmount(), event.getTransactionId());
                    log.info("Successfully processed deposit for account: {}", payload.getAccountId());
                }
                case "WithdrawRequested" -> {
                    String json = objectMapper.writeValueAsString(event.getPayload());
                    WithdrawRequested payload = objectMapper.readValue(json, WithdrawRequested.class);
                    accountService.withdraw(payload.getAccountId(), payload.getAmount(), event.getTransactionId());
                    log.info("Successfully processed withdrawal for account: {}", payload.getAccountId());
                }
                case "TransferRequested" -> {
                    String json = objectMapper.writeValueAsString(event.getPayload());
                    TransferRequested payload = objectMapper.readValue(json, TransferRequested.class);
                    accountService.reserveMoney(payload.getFromAccountId(), payload.getAmount(),
                            event.getTransactionId());
                    log.info("Successfully reserved money for transfer from: {}", payload.getFromAccountId());
                }
                default -> log.warn("Unhandled event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing transaction command. Message: {}", message, e);
        }
    }
}
