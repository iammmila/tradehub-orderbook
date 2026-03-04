package com.ab.apigateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response returned by auth-service /introspect.
 * Used by gateway to pass user context to other services.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntrospectResponse {
    private Long userId;
    private String username;
    private List<String> roles;
}