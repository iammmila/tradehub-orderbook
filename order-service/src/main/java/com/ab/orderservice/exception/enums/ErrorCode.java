package com.ab.orderservice.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    //USER
    USER_NOT_VERIFIED("USER_NOT_VERIFIED", "User has not been verified yet"),

    //ACCESS
    ACCESS_DENIED("ACCESS_DENIED", "You are not allowed to perform this action."),

    // ORDER
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "Order not found"),
    ORDER_CANNOT_CANCEL("ORDER_CANNOT_CANCEL", "Only orders with status NEW can be cancelled"),
    ORDER_CANNOT_REPLACE("ORDER_CANNOT_REPLACE", "This order cannot be replaced in its current status."),
    ORDER_REPLACE_INVALID_QUANTITY("ORDER_REPLACE_INVALID_QUANTITY", "New quantity cannot be less than already filled quantity."),
    ORDER_PRICE_REQUIRED("ORDER_PRICE_REQUIRED", "Price is required for LIMIT/HIDDEN_LIMIT orders"),
    INSTRUMENT_REQUIRED("INSTRUMENT_REQUIRED", "Instrument is required"),

    // ROUTE
    EXCHANGE_NOT_SUPPORTED("EXCHANGE_NOT_SUPPORTED", "Exchange is not supported"),

    // VALIDATION
    VALIDATION_FAILED("VALIDATION_FAILED", "Validation failed"),
    PRICE_NOT_ALLOWED_FOR_MARKET("PRICE_NOT_ALLOWED_FOR_MARKET", "Price is not allowed for Market."),
    MIN_EXEC_SIZE_NOT_ALLOWED("MIN_EXEC_SIZE_NOT_ALLOWED", "Min Exec Size is not allowed."),
    MIN_EXEC_SIZE_REQUIRED("MIN_EXEC_SIZE_REQUIRED", "Min Exec Size is required."),
    MIN_EXEC_SIZE_TOO_LARGE("MIN_EXEC_SIZE_TOO_LARGE", "Min Exec Size is greater than Quantity"),

    // GENERAL
    INTERNAL_ERROR("INTERNAL_ERROR", "Unexpected error occurred");

    private final String code;
    private final String message;
}
