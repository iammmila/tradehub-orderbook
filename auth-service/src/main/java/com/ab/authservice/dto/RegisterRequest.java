package com.ab.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 5, max = 15, message = "Username must be 5-15 characters")
    private String username;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "First name cannot be blank")
    @Size(max = 50, message = "First name must be max 50 characters")
    @Pattern(
            regexp = "^[\\p{L}]+([\\p{L} '\\-]*[\\p{L}])?$",
            message = "First name can contain only letters, spaces, apostrophe, or hyphen"
    )
    private String firstName;

    @NotBlank(message = "Last name cannot be blank")
    @Size(max = 50, message = "Last name must be max 50 characters")
    @Pattern(
            regexp = "^[\\p{L}]+([\\p{L} '\\-]*[\\p{L}])?$",
            message = "Last name can contain only letters, spaces, apostrophe, or hyphen"
    )
    private String lastName;
}