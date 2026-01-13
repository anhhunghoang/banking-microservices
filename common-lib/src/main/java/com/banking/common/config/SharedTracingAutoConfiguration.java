package com.banking.common.config;

import com.banking.common.tracing.TracingProducerInterceptor;
import com.banking.common.tracing.TracingService;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;

import java.util.Collections;

@AutoConfiguration
@ConditionalOnClass({ Tracer.class, ProducerConfig.class })
public class SharedTracingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TracingService tracingService(Tracer tracer, Propagator propagator) {
        return new TracingService(tracer, propagator);
    }

    @Bean
    public DefaultKafkaProducerFactoryCustomizer tracingKafkaCustomizer() {
        return producerFactory -> producerFactory.updateConfigs(
                Collections.singletonMap(
                        ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
                        TracingProducerInterceptor.class.getName()));
    }
}
