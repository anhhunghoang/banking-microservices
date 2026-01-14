package com.banking.account.event;

import com.banking.common.constant.ServiceGroups;
import com.banking.common.constant.Topics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AccountDltListener {

    @KafkaListener(topics = Topics.TRANSACTIONS_COMMANDS + ".DLT", groupId = ServiceGroups.ACCOUNT_DLT_GROUP)
    public void handleDlt(String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(value = "x-exception-message", required = false) byte[] exceptionMessage,
            @Header(value = "x-exception-stacktrace", required = false) byte[] stackTrace) {

        String error = exceptionMessage != null ? new String(exceptionMessage) : "Unknown error";

        log.error("🛑 RECEIVED FAILED MESSAGE IN DLT Topic: {} | Original Message: {}", topic, message);
        log.error("🛑 Failure Reason: {}", error);

        // TODO: In a production system, you would:
        // 1. Save this to a 'failed_events' database table.
        // 2. Trigger an alert (Slack/PagerDuty).
        // 3. Provide a UI/API for an admin to 'Replay' this event after fixing the
        // issue.
    }
}
