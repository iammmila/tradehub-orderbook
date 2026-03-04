package com.ab.authservice.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyEmailRequest {
    @NotBlank
    private String token;
}