package com.banking.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final String errorCode;

    public BusinessException(String initialMsg, String errorCode) {
        super(initialMsg);
        this.errorCode = errorCode;
    }
}
