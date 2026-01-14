package com.banking.transaction.event;

import com.banking.common.constant.ServiceGroups;
import com.banking.common.constant.Topics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TransactionDltListener {

    @KafkaListener(topics = Topics.ACCOUNTS_EVENTS + ".DLT", groupId = ServiceGroups.TRANSACTION_DLT_GROUP)
    public void handleDlt(String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(value = "x-exception-message", required = false) byte[] exceptionMessage) {

        String error = exceptionMessage != null ? new String(exceptionMessage) : "Unknown error";

        log.error("🛑 TRANSACTION SERVICE DLT: Failed message in Topic: {} | Original Message: {}", topic, message);
        log.error("🛑 Failure Reason: {}", error);
    }
}
