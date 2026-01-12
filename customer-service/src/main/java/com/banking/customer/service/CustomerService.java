package com.banking.customer.service;

import com.banking.customer.dto.CreateCustomerRequest;
import com.banking.customer.dto.CustomerResponse;

import java.util.UUID;

public interface CustomerService {
    CustomerResponse createCustomer(CreateCustomerRequest request);

    CustomerResponse getCustomer(UUID id);
}
