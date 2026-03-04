package com.ab.authservice.service.user;

import com.ab.authservice.dto.user.ChangePasswordRequest;
import com.ab.authservice.exception.BadRequestException;
import com.ab.authservice.exception.NotFoundException;
import com.ab.authservice.exception.enums.ErrorCode;
import com.ab.authservice.model.User;
import com.ab.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPasswordService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
}
