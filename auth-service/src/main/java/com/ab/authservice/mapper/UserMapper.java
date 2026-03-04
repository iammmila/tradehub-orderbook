package com.ab.authservice.mapper;

import com.ab.authservice.dto.user.UserDto;
import com.ab.authservice.dto.user.UserResponse;
import com.ab.authservice.model.User;
import org.springframework.stereotype.Component;

/**
 * Maps User entity -> UserResponse DTO
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) return null;

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .isVerified(user.isVerified())
                .build();
    }

    public UserDto toDto(User user) {
        if (user == null) return null;
        return new UserDto(user.getId(), user.getUsername());
    }
}