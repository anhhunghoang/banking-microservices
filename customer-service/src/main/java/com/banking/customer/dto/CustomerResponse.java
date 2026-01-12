package com.banking.customer.dto;

import com.banking.customer.model.Customer;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CustomerResponse {
    private UUID id;
    private String name;
    private String email;
    private Customer.CustomerStatus status;
    private LocalDateTime createdAt;
}
