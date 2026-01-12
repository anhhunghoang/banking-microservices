package com.banking.transaction.service;

import com.banking.transaction.dto.TransactionRequest;
import com.banking.transaction.dto.TransactionResponse;

import java.util.UUID;

public interface TransactionService {
    TransactionResponse createDeposit(TransactionRequest request);

    TransactionResponse createWithdrawal(TransactionRequest request);

    TransactionResponse getTransaction(UUID id);
}
