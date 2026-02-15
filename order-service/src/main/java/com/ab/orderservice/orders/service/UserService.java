package com.ab.orderservice.orders.service;

import com.ab.orderservice.common.exception.BadRequestException;
import com.ab.orderservice.common.exception.ErrorCode;
import com.ab.orderservice.common.exception.NotFoundException;
import com.ab.orderservice.orders.dto.user.CreateUserRequest;
import com.ab.orderservice.orders.dto.user.UserResponse;
import com.ab.orderservice.orders.model.User;
import com.ab.orderservice.orders.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.findByEmail(request.getEmail().trim().toLowerCase()).isPresent()) {
            throw new BadRequestException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
        }
        User user = User.builder()
                .email(request.getEmail().trim().toLowerCase())
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .password(request.getPassword())
                .createdAt(LocalDateTime.now())
                .build();

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public List<UserResponse> getUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
