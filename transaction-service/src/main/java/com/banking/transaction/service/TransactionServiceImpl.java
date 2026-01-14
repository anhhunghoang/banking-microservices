package com.banking.transaction.service;

import com.banking.common.event.BaseEvent;
import com.banking.common.event.DepositRequested;
import com.banking.common.event.TransferRequested;
import com.banking.common.event.WithdrawRequested;
import com.banking.common.constant.AggregateTypes;
import com.banking.common.constant.EventTypes;
import com.banking.common.constant.ErrorCodes;
import com.banking.common.exception.BusinessException;
import com.banking.common.tracing.TracingService;
import com.banking.transaction.dto.TransactionRequest;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.dto.TransferRequest;
import com.banking.transaction.model.OutboxEvent;
import com.banking.transaction.model.Transaction;
import com.banking.transaction.repository.OutboxRepository;
import com.banking.transaction.repository.TransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

        private final TransactionRepository transactionRepository;
        private final OutboxRepository outboxRepository;
        private final ObjectMapper objectMapper;
        private final TracingService tracingService;

        @Override
        @Transactional
        public TransactionResponse createDeposit(TransactionRequest request) {
                Transaction transaction = Transaction.builder()
                                .accountId(request.getAccountId())
                                .amount(request.getAmount())
                                .currency(request.getCurrency())
                                .type(Transaction.TransactionType.DEPOSIT)
                                .status(Transaction.TransactionStatus.PENDING)
                                .build();

                Transaction savedTransaction = transactionRepository.save(transaction);

                saveOutboxEvent(savedTransaction, EventTypes.DEPOSIT_REQUESTED,
                                DepositRequested.builder()
                                                .accountId(savedTransaction.getAccountId())
                                                .amount(savedTransaction.getAmount())
                                                .currency(savedTransaction.getCurrency())
                                                .build());

                return mapToResponse(savedTransaction);
        }

        @Override
        @Transactional
        public TransactionResponse createWithdrawal(TransactionRequest request) {
                Transaction transaction = Transaction.builder()
                                .accountId(request.getAccountId())
                                .amount(request.getAmount())
                                .currency(request.getCurrency())
                                .type(Transaction.TransactionType.WITHDRAWAL)
                                .status(Transaction.TransactionStatus.PENDING)
                                .build();

                Transaction savedTransaction = transactionRepository.save(transaction);

                saveOutboxEvent(savedTransaction, EventTypes.WITHDRAW_REQUESTED,
                                WithdrawRequested.builder()
                                                .accountId(savedTransaction.getAccountId())
                                                .amount(savedTransaction.getAmount())
                                                .currency(savedTransaction.getCurrency())
                                                .build());

                return mapToResponse(savedTransaction);
        }

        @Override
        @Transactional
        public TransactionResponse createTransfer(TransferRequest request) {
                Transaction transaction = Transaction.builder()
                                .fromAccountId(request.getFromAccountId())
                                .toAccountId(request.getToAccountId())
                                .amount(request.getAmount())
                                .currency(request.getCurrency())
                                .type(Transaction.TransactionType.TRANSFER)
                                .status(Transaction.TransactionStatus.PENDING)
                                .build();

                Transaction savedTransaction = transactionRepository.save(transaction);

                saveOutboxEvent(savedTransaction, EventTypes.TRANSFER_REQUESTED,
                                TransferRequested.builder()
                                                .fromAccountId(savedTransaction.getFromAccountId())
                                                .toAccountId(savedTransaction.getToAccountId())
                                                .amount(savedTransaction.getAmount())
                                                .currency(savedTransaction.getCurrency())
                                                .build());

                return mapToResponse(savedTransaction);
        }

        @Override
        @Transactional(readOnly = true)
        public TransactionResponse getTransaction(UUID id) {
                return transactionRepository.findById(id)
                                .map(this::mapToResponse)
                                .orElseThrow(() -> new BusinessException("Transaction not found",
                                                "TRANSACTION_NOT_FOUND"));
        }

        private void saveOutboxEvent(Transaction transaction, String eventType, Object payload) {
                try {
                        BaseEvent<?> event = BaseEvent.builder()
                                        .eventId(UUID.randomUUID())
                                        .eventType(eventType)
                                        .eventVersion(1)
                                        .aggregateType(AggregateTypes.TRANSACTION)
                                        .aggregateId(transaction.getId())
                                        .transactionId(transaction.getId())
                                        .requestId(UUID.randomUUID())
                                        .correlationId(UUID.randomUUID())
                                        .traceId(tracingService.getCurrentTraceId())
                                        .timestamp(LocalDateTime.now())
                                        .payload(payload)
                                        .build();

                        OutboxEvent outboxEvent = OutboxEvent.builder()
                                        .aggregateType(AggregateTypes.TRANSACTION)
                                        .aggregateId(transaction.getId())
                                        .eventType(eventType)
                                        .payload(objectMapper.writeValueAsString(event))
                                        .status(OutboxEvent.OutboxStatus.PENDING)
                                        .build();

                        outboxRepository.save(outboxEvent);
                } catch (JsonProcessingException e) {
                        log.error("Error serializing outbox event", e);
                        throw new BusinessException("Error serializing outbox event", ErrorCodes.SERIALIZATION_ERROR);
                }
        }

        private TransactionResponse mapToResponse(Transaction transaction) {
                return TransactionResponse.builder()
                                .id(transaction.getId())
                                .accountId(transaction.getAccountId())
                                .amount(transaction.getAmount())
                                .type(transaction.getType().name())
                                .status(transaction.getStatus().name())
                                .traceId(tracingService.getCurrentTraceId())
                                .build();
        }
}
