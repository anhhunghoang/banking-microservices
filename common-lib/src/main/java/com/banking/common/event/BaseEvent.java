package com.banking.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseEvent<T> {

    @JsonProperty("event_id")
    private UUID eventId;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("event_version")
    private Integer eventVersion;

    @JsonProperty("aggregate_type")
    private String aggregateType;

    @JsonProperty("aggregate_id")
    private UUID aggregateId;

    @JsonProperty("transaction_id")
    private UUID transactionId;

    @JsonProperty("request_id")
    private UUID requestId;

    @JsonProperty("correlation_id")
    private UUID correlationId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime timestamp;

    private T payload;
}
