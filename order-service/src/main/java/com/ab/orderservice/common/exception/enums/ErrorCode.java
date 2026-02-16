package com.ab.orderservice.common.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // ORDER
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "Order not found"),
    ORDER_CANNOT_CANCEL("ORDER_CANNOT_CANCEL", "Only orders with status NEW can be cancelled"),

    // USER
    USER_NOT_FOUND("USER_NOT_FOUND", "User not found"),
    USER_EMAIL_ALREADY_EXISTS("USER_EMAIL_ALREADY_EXISTS", "Email already exists"),
    USER_USERNAME_ALREADY_EXISTS("USER_USERNAME_ALREADY_EXISTS", "Username already exists"),
    ROLE_NOT_FOUND("ROLE_NOT_FOUND", "Role not found"),
    ROLE_ALREADY_EXISTS("ROLE_ALREADY_EXISTS", "Role already exists"),

    // VALIDATION
    VALIDATION_FAILED("VALIDATION_FAILED", "Validation failed"),

    // GENERAL
    INTERNAL_ERROR("INTERNAL_ERROR", "Unexpected error occurred");
    private final String code;
    private final String message;
}
