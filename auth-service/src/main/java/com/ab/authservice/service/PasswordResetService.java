package com.ab.authservice.service;

import com.ab.authservice.config.TokenHasher;
import com.ab.authservice.exception.BadRequestException;
import com.ab.authservice.exception.enums.ErrorCode;
import com.ab.authservice.messaging.Channel;
import com.ab.authservice.messaging.NotificationCommand;
import com.ab.authservice.messaging.NotificationService;
import com.ab.authservice.messaging.Template;
import com.ab.authservice.model.PasswordResetToken;
import com.ab.authservice.model.User;
import com.ab.authservice.repository.PasswordResetTokenRepository;
import com.ab.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TokenHasher tokenHasher;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Value("${app.frontend.reset-url}")
    private String resetUrl; // e.g. http://localhost:3000/reset-password?token=%s

    // If email exists: creates token and sends email. If not: does nothing (prevents email enumeration).
    public void requestReset(String email, String ip, String userAgent) {
        var userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) return;

        User user = userOpt.get();

        // Invalidate old active tokens for this user (only latest token stays valid)
        passwordResetTokenRepository.invalidateAllActiveForUser(user.getId(), LocalDateTime.now());

        // Generate a long random token, store only hashed version in DB
        String rawToken = UUID.randomUUID().toString() + UUID.randomUUID(); // longer token
        String tokenHash = tokenHasher.sha256Hex(rawToken);

        PasswordResetToken prt = new PasswordResetToken();
        prt.setUser(user);
        prt.setTokenHash(tokenHash);
        prt.setCreatedAt(LocalDateTime.now());
        prt.setExpiresAt(LocalDateTime.now().plusMinutes(20));
        prt.setRequestIp(ip);
        prt.setUserAgent(userAgent);

        passwordResetTokenRepository.save(prt);

        // Send link to frontend (raw token goes to user, hash stays in DB)
        String link = resetUrl.formatted(URLEncoder.encode(rawToken, StandardCharsets.UTF_8));
        notificationService.send(
                NotificationCommand.builder()
                        .channel(Channel.EMAIL)
                        .to(user.getEmail())
                        .template(Template.PASSWORD_RESET)
                        .subject("Reset your password")
                        .variables(Map.of("resetLink", link))
                        .locale("en")
                        .build()
        );
    }

    // Send link to frontend (raw token goes to user, hash stays in DB)
    public void resetPassword(String rawToken, String newPassword) {
        String tokenHash = tokenHasher.sha256Hex(rawToken);

        PasswordResetToken prt = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException(ErrorCode.RESET_TOKEN_EXPIRED));

        if (prt.getUsedAt() != null) throw new BadRequestException(ErrorCode.RESET_TOKEN_USED);
        if (prt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException(ErrorCode.RESET_TOKEN_EXPIRED);
        }

        // Update user password (store BCrypt hash)
        User user = prt.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used (one-time token)
        prt.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(prt);
    }
}
