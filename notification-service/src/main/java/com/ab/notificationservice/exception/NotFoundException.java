package com.ab.notificationservice.exception;

import com.ab.notificationservice.exception.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {
    public NotFoundException(ErrorCode errorCode) {
        super(HttpStatus.NOT_FOUND, errorCode);
    }
}
