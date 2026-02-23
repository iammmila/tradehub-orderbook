package com.ab.authservice.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRoleRequest {
    @NotBlank
    @Pattern(regexp = "^[A-Za-z_]{3,30}$", message = "role name must be 3-30 letters/underscore")
    private String name; // e.g. "ADMIN" or "USER"
}
