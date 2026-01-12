package com.banking.account.event;

import com.banking.account.service.AccountService;
import com.banking.common.event.BaseEvent;
import com.banking.common.event.DepositRequested;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AccountEventListener {

    private final AccountService accountService;

    @KafkaListener(topics = "transactions.commands", groupId = "account-service-group")
    public void handleDepositRequested(BaseEvent<DepositRequested> event) {
        log.info("Received DepositRequested event: {}", event.getCorrelationId());
        DepositRequested payload = event.getPayload();
        accountService.deposit(payload.getAccountId(), payload.getAmount());
    }
}
