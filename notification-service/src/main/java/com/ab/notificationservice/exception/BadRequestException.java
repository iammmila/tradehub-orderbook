package com.ab.notificationservice.exception;

import com.ab.notificationservice.exception.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {
    public BadRequestException(ErrorCode errorCode) {
        super(HttpStatus.BAD_REQUEST, errorCode);
    }
}
