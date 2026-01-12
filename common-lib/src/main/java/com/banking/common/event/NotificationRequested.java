package com.banking.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequested {
    @JsonProperty("user_id")
    private UUID userId;
    private String type; // SMS, EMAIL
    private String destination; // phone number or email
    private String message;
}
