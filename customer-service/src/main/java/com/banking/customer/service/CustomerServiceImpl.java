package com.banking.customer.service;

import com.banking.common.exception.BusinessException;
import com.banking.customer.dto.CreateCustomerRequest;
import com.banking.customer.dto.CustomerResponse;
import com.banking.customer.model.Customer;
import com.banking.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        log.info("Creating customer with email: {}", request.getEmail());

        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Customer with email " + request.getEmail() + " already exists",
                    "CUSTOMER_EXISTS");
        }

        Customer customer = Customer.builder()
                .name(request.getName())
                .email(request.getEmail())
                .status(Customer.CustomerStatus.ACTIVE)
                .build();

        customer = customerRepository.save(customer);
        log.info("Customer created with ID: {}", customer.getId());

        return mapToResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Customer not found", "CUSTOMER_NOT_FOUND"));

        return mapToResponse(customer);
    }

    private CustomerResponse mapToResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .email(customer.getEmail())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .build();
    }
}
