package com.banking.account.service;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.event.AccountEventProducer;
import com.banking.account.model.Account;
import com.banking.account.repository.AccountRepository;
import com.banking.common.event.AccountCreated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountEventProducer eventProducer;

    @InjectMocks
    private AccountServiceImpl accountService;

    private UUID customerId;
    private CreateAccountRequest createRequest;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        createRequest = CreateAccountRequest.builder()
                .customerId(customerId)
                .initialBalance(BigDecimal.valueOf(100.00))
                .build();
    }

    @Test
    void shouldCreateAccount_WhenRequestIsValid() {
        // Arrange
        UUID accountId = UUID.randomUUID();
        Account savedAccount = Account.builder()
                .id(accountId)
                .customerId(customerId)
                .balance(BigDecimal.valueOf(100.00))
                .status(Account.AccountStatus.ACTIVE)
                .build();

        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        // Act
        AccountResponse response = accountService.createAccount(createRequest);

        // Assert
        assertThat(response.getCustomerId()).isEqualTo(customerId);
        assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        assertThat(response.getStatus()).isEqualTo("ACTIVE");

        verify(accountRepository).save(any(Account.class));
        verify(eventProducer).sendAccountCreated(any(AccountCreated.class));
    }
}
