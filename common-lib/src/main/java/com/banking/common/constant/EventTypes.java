package com.banking.common.constant;

public class EventTypes {
    private EventTypes() {}

    // Transaction Commands
    public static final String DEPOSIT_REQUESTED = "DepositRequested";
    public static final String WITHDRAW_REQUESTED = "WithdrawRequested";
    public static final String TRANSFER_REQUESTED = "TransferRequested";
    public static final String REFUND_REQUESTED = "RefundRequested";

    // Account Events
    public static final String ACCOUNT_CREATED = "AccountCreated";
    public static final String MONEY_RESERVED = "MoneyReserved";
    public static final String MONEY_CREDITED = "MoneyCredited";
    public static final String MONEY_DEBITED = "MoneyDebited";
    public static final String RESERVATION_FAILED = "ReservationFailed";
    public static final String REFUND_COMPLETED = "RefundCompleted";

    // Transaction Events
    public static final String TRANSACTION_COMPLETED = "TransactionCompleted";
    public static final String TRANSACTION_FAILED = "TransactionFailed";
}
