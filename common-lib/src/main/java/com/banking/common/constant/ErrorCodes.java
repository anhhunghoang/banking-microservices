package com.banking.common.constant;

public class ErrorCodes {
    private ErrorCodes() {}

    public static final String INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS";
    public static final String ACCOUNT_FROZEN = "ACCOUNT_FROZEN";
    public static final String ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";
    public static final String CUSTOMER_NOT_FOUND = "CUSTOMER_NOT_FOUND";
    public static final String TRANSACTION_NOT_FOUND = "TRANSACTION_NOT_FOUND";
    public static final String CUSTOMER_EXISTS = "CUSTOMER_EXISTS";
    public static final String SERIALIZATION_ERROR = "SERIALIZATION_ERROR";
}
