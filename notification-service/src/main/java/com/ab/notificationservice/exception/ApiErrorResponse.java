package com.ab.notificationservice.exception;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;       // HTTP status name, e.g. BAD_REQUEST
    private String code;        // ErrorCode.code
    private String message;     // ErrorCode.message
    private String path;

    @Singular
    private List<FieldError> fieldErrors;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
    }
}
