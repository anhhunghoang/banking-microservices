package com.banking.common.tracing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class TracingProducerInterceptor implements ProducerInterceptor<Object, Object> {

    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("\"trace_id\"\\s*:\\s*\"([^\"]+)\"");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ProducerRecord<Object, Object> onSend(ProducerRecord<Object, Object> producerRecord) {
        Object value = producerRecord.value();
        String traceId = extractTraceId(value);

        if (traceId != null) {
            log.info("Kafka Interceptor: Found traceId {} in payload. Injecting into headers.", traceId);
            // We inject the traceId directly into the headers so the consumer's
            // observation logic (which looks at headers) picks it up automatically!
            // Format: traceparent (W3C standard)
            String traceparent = String.format("00-%s-%s-01", traceId, "0000000000000001");
            producerRecord.headers().add("traceparent", traceparent.getBytes());
        }

        return producerRecord;
    }

    private String extractTraceId(Object value) {
        if (value == null)
            return null;

        String json = null;
        if (value instanceof String jsonStr) {
            json = jsonStr;
        } else {
            try {
                // If it's an object, we serialize it to check for trace_id.
                // Outbox pattern usually stores payload as a JSON String already.
                json = objectMapper.writeValueAsString(value);
            } catch (Exception e) {
                log.debug("Failed to serialize Kafka record value for trace extraction", e);
            }
        }

        if (json != null) {
            Matcher matcher = TRACE_ID_PATTERN.matcher(json);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        // No-op: tracing is handled during record creation and sending
    }

    @Override
    public void close() {
        // No-op: no resources to release
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // No-op: no custom configuration needed
    }
}
