package com.ab.orderservice.common.exception;

import com.ab.orderservice.common.exception.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApiException {
    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode, HttpStatus.UNAUTHORIZED);
    }
}
