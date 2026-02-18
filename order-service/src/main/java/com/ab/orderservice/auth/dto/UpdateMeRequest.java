package com.ab.orderservice.auth.dto;

import jakarta.validation.constraints.Email;
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
public class UpdateMeRequest {
    @Size(min = 5, max = 15, message = "Username must be 5-15 characters")
    private String username;

    @Email(message = "Email must be a valid email address")
    private String email;

    @Size(max = 50, message = "First name must be max 50 characters")
    @Pattern(
            regexp = "^[\\p{L}]+([\\p{L} '\\-]*[\\p{L}])?$",
            message = "First name can contain only letters, spaces, apostrophe, or hyphen"
    )
    private String firstName;

    @Size(max = 50, message = "Last name must be max 50 characters")
    @Pattern(
            regexp = "^[\\p{L}]+([\\p{L} '\\-]*[\\p{L}])?$",
            message = "Last name can contain only letters, spaces, apostrophe, or hyphen"
    )
    private String lastName;
}
