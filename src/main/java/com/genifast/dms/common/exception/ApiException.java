package com.genifast.dms.common.exception;

import lombok.Getter;

import com.genifast.dms.common.constant.ErrorCode;

@Getter
public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String message;

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
    }
}