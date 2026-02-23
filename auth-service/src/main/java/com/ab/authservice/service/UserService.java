package com.ab.authservice.service;

import com.ab.authservice.dto.UpdateMeRequest;
import com.ab.authservice.dto.user.ChangePasswordRequest;
import com.ab.authservice.dto.user.UserResponse;
import com.ab.authservice.model.User;
import com.ab.authservice.repository.UserRepository;
import com.ab.authservice.exception.BadRequestException;
import com.ab.authservice.exception.NotFoundException;
import com.ab.authservice.exception.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getMe(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        return toResponse(user);
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

    public UserResponse updateMe(String currentUsername, UpdateMeRequest req) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        // username optional
        if (req.getUsername() != null) {
            String newUsername = req.getUsername().trim();

            if (newUsername.isEmpty()) {
                throw new BadRequestException(ErrorCode.USERNAME_EMPTY);
            }

            if (!user.getUsername().equals(newUsername)) {
                if (userRepository.existsByUsername(newUsername)) {
                    throw new BadRequestException(ErrorCode.USER_USERNAME_ALREADY_EXISTS);
                }
                user.setUsername(newUsername);
            }
        }

        // username optional
        if (req.getEmail() != null) {
            String newEmail = req.getEmail().trim();

            if (newEmail.isEmpty()) {
                throw new BadRequestException(ErrorCode.EMAIL_EMPTY);
            }

            if (!user.getEmail().equals(newEmail)) {
                if (userRepository.existsByEmail(newEmail)) {
                    throw new BadRequestException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
                }
                user.setEmail(newEmail);
            }
        }

        // firstName optional
        if (req.getFirstName() != null) {
            String newFirstName = req.getFirstName().trim();
            if (newFirstName.isEmpty()) {
                throw new BadRequestException(ErrorCode.FIRSTNAME_EMPTY);
            }
            user.setFirstName(newFirstName);
        }

        // lastName optional
        if (req.getLastName() != null) {
            String newLastName = req.getLastName().trim();
            if (newLastName.isEmpty()) {
                throw new BadRequestException(ErrorCode.LASTNAME_EMPTY);
            }
            user.setLastName(newLastName);
        }

        userRepository.save(user);
        return toResponse(user);
    }

    public void changePassword(String username, ChangePasswordRequest req) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
        String currentPassword = req.getCurrentPassword().trim();
        String newPassword = req.getNewPassword().trim();

        // current password must be correct
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException(ErrorCode.USER_PASSWORD_INCORRECT);
        }
        //new password must be different from current password
        if (currentPassword.equals(newPassword)) {
            throw new BadRequestException(ErrorCode.USER_PASSWORD_SAME);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }
}
