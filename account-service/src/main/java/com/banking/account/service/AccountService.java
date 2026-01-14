package com.banking.account.service;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.CreateAccountRequest;
import java.math.BigDecimal;
import java.util.UUID;

public interface AccountService {
    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse getAccount(UUID id);

    void deposit(UUID id, BigDecimal amount, UUID transactionId);

    void withdraw(UUID id, BigDecimal amount, UUID transactionId);

    void reserveMoney(UUID id, BigDecimal amount, UUID transactionId);

    void debitMoney(UUID id, java.math.BigDecimal amount, UUID transactionId);

    void creditMoney(UUID id, java.math.BigDecimal amount, UUID transactionId);

    void refund(UUID id, java.math.BigDecimal amount, UUID transactionId);
}
