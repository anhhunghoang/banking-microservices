package com.banking.account.event;

import com.banking.account.repository.ProcessedEventRepository;
import com.banking.account.service.AccountService;
import com.banking.common.constant.EventTypes;
import com.banking.common.event.BaseEvent;
import com.banking.common.event.DepositRequested;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountEventListenerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private ObjectMapper objectMapper;

    private AccountEventListener accountEventListener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        accountEventListener = new AccountEventListener(accountService, objectMapper, processedEventRepository);
    }

    @Test
    void shouldProcessDepositRequested() throws Exception {
        // Arrange
        UUID accountId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(50.00);
        UUID transactionId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        DepositRequested payload = DepositRequested.builder()
                .accountId(accountId)
                .amount(amount)
                .currency("USD")
                .build();

        BaseEvent<Object> event = BaseEvent.builder()
                .eventId(eventId)
                .eventType(EventTypes.DEPOSIT_REQUESTED)
                .transactionId(transactionId)
                .payload(payload)
                .build();

        String message = objectMapper.writeValueAsString(event);

        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        // Act
        accountEventListener.handleTransactionCommands(message);

        // Assert
        verify(accountService).deposit(accountId, amount, transactionId);
        verify(processedEventRepository).save(any());
    }
}
