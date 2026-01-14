package com.banking.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@AutoConfiguration
@ConditionalOnClass(DefaultErrorHandler.class)
public class SharedKafkaAutoConfiguration {

    @Bean
    public CommonErrorHandler errorHandler(KafkaOperations<Object, Object> template) {
        // After 3 retries, the message will be sent to a topic named
        // {originalTopic}.DLT
        return new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(template),
                new FixedBackOff(1000L, 3));
    }
}
