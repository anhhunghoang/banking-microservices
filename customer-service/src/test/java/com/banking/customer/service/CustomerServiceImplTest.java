package com.banking.customer.service;

import com.banking.common.exception.BusinessException;
import com.banking.customer.dto.CreateCustomerRequest;
import com.banking.customer.dto.CustomerResponse;
import com.banking.customer.model.Customer;
import com.banking.customer.repository.CustomerRepository;
import com.banking.common.event.CustomerCreated;
import com.banking.customer.event.CustomerEventProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Mock
    private CustomerEventProducer customerEventProducer;

    @Test
    void shouldCreateCustomer_WhenEmailIsUnique() {
        // Arrange
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .build();

        when(customerRepository.existsByEmail(anyString())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            c.setCreatedAt(LocalDateTime.now());
            return c;
        });

        // Act
        CustomerResponse response = customerService.createCustomer(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        assertThat(response.getStatus()).isEqualTo(Customer.CustomerStatus.ACTIVE);

        verify(customerRepository).existsByEmail(request.getEmail());
        verify(customerRepository).save(any(Customer.class));
        verify(customerEventProducer).sendCustomerCreated(any(CustomerCreated.class));
    }

    @Test
    void shouldThrowException_WhenEmailAlreadyExists() {
        // Arrange
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .build();

        when(customerRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void shouldGetCustomer_WhenIdExists() {
        // Arrange
        UUID id = UUID.randomUUID();
        Customer customer = Customer.builder()
                .id(id)
                .name("Alice")
                .email("alice@example.com")
                .status(Customer.CustomerStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        // Act
        CustomerResponse response = customerService.getCustomer(id);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getEmail()).isEqualTo(customer.getEmail());
    }

    @Test
    void shouldThrowException_WhenIdDoesNotExist() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customerService.getCustomer(id))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Customer not found");
    }
}
