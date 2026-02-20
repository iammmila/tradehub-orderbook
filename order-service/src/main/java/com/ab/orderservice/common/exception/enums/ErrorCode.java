package com.ab.orderservice.common.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // AUTH
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", "Username or password is wrong!"),

    // ORDER
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "Order not found"),
    ORDER_CANNOT_CANCEL("ORDER_CANNOT_CANCEL", "Only orders with status NEW can be cancelled"),
    ACCESS_DENIED("ACCESS_DENIED", "You are not allowed to perform this action."),
    // USER
    USER_NOT_FOUND("USER_NOT_FOUND", "User not found"),
    USER_EMAIL_ALREADY_EXISTS("USER_EMAIL_ALREADY_EXISTS", "Email already exists"),
    USER_USERNAME_ALREADY_EXISTS("USER_USERNAME_ALREADY_EXISTS", "Username already exists"),
    USER_PASSWORD_INCORRECT("USER_PASSWORD_INCORRECT", "Current password is incorrect"),
    USER_PASSWORD_SAME("USER_PASSWORD_SAME", "New password can  not be same with current password"),
    ROLE_NOT_FOUND("ROLE_NOT_FOUND", "Role not found"),
    ROLE_ALREADY_EXISTS("ROLE_ALREADY_EXISTS", "Role already exists"),

    // VALIDATION
    VALIDATION_FAILED("VALIDATION_FAILED", "Validation failed"),
    USERNAME_EMPTY("USERNAME_EMPTY", "Username can not be empty"),
    EMAIL_EMPTY("EMAIL_EMPTY", "email can not be empty"),
    FIRSTNAME_EMPTY("FIRSTNAME_EMPTY", "firstname can not be empty"),
    LASTNAME_EMPTY("LASTNAME_EMPTY", "lastname can not be empty"),

    // GENERAL
    INTERNAL_ERROR("INTERNAL_ERROR", "Unexpected error occurred");

    private final String code;
    private final String message;
}
