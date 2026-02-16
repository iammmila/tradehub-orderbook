package com.ab.orderservice.common.exception;

import com.ab.orderservice.common.exception.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {
    public BadRequestException(ErrorCode errorCode) {
        super(errorCode, HttpStatus.BAD_REQUEST);
    }
}