package com.ab.notificationservice.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    //NOTIFICATION
    NOTIFICATION_NOT_FOUND("NOTIFICATION_NOT_FOUND", "Notification not found"),

    // ACCESS
    ACCESS_DENIED("ACCESS_DENIED", "You are not allowed to perform this action."),

    // VALIDATION
    VALIDATION_FAILED("VALIDATION_FAILED", "Validation failed"),

    // GENERAL
    INTERNAL_ERROR("INTERNAL_ERROR", "Unexpected error occurred");

    private final String code;
    private final String message;
}
