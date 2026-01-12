package com.banking.account.event;

import com.banking.account.service.AccountService;
import com.banking.common.event.BaseEvent;
import com.banking.common.event.DepositRequested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountEventListenerTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountEventListener accountEventListener;

    @Test
    void shouldProcessDepositRequested() {
        // Arrange
        UUID accountId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(50.00);

        DepositRequested payload = DepositRequested.builder()
                .accountId(accountId)
                .amount(amount)
                .currency("USD")
                .build();

        BaseEvent<DepositRequested> event = new BaseEvent<>();
        event.setPayload(payload);
        event.setEventType("DepositRequested");

        // Act
        accountEventListener.handleDepositRequested(event);

        // Assert
        verify(accountService).deposit(accountId, amount);
    }
}
