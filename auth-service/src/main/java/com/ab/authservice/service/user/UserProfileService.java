package com.ab.authservice.service.user;

import com.ab.authservice.dto.UpdateMeRequest;
import com.ab.authservice.dto.user.UserResponse;
import com.ab.authservice.exception.BadRequestException;
import com.ab.authservice.exception.NotFoundException;
import com.ab.authservice.exception.enums.ErrorCode;
import com.ab.authservice.mapper.UserMapper;
import com.ab.authservice.model.User;
import com.ab.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

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
        return userMapper.toResponse(user);
    }
}
