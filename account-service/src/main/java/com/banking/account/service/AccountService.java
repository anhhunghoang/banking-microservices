package com.banking.account.service;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.CreateAccountRequest;

import java.util.UUID;

public interface AccountService {
    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse getAccount(UUID id);

    void deposit(UUID id, java.math.BigDecimal amount);

    void withdraw(UUID id, java.math.BigDecimal amount);

    void reserveMoney(UUID id, java.math.BigDecimal amount, UUID transactionId);

    void debitMoney(UUID id, java.math.BigDecimal amount, UUID transactionId);

    void creditMoney(UUID id, java.math.BigDecimal amount, UUID transactionId);
}
