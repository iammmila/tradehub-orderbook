package com.ab.apigateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntrospectResponse {
    private Long userId;
    private String username;
    private List<String> roles;
}