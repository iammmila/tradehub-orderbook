package com.ab.authservice.service;

import com.ab.authservice.config.TokenHasher;
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
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TokenHasher tokenHasher;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.reset-url}")
    private String resetUrl; // e.g. http://localhost:3000/reset-password?token=%s

    public void requestReset(String email, String ip, String userAgent) {
        var userOpt = userRepository.findByEmailIgnoreCase(email);

        if (userOpt.isEmpty()) return;

        User user = userOpt.get();

        passwordResetTokenRepository.invalidateAllActiveForUser(user.getId(), LocalDateTime.now());

        // Create token
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

        String link = resetUrl.formatted(URLEncoder.encode(rawToken, StandardCharsets.UTF_8));
        emailService.sendResetPasswordEmail(user.getEmail(), link);
    }

    public void resetPassword(String rawToken, String newPassword) {
        String tokenHash = tokenHasher.sha256Hex(rawToken);

        PasswordResetToken prt = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (prt.getUsedAt() != null) throw new RuntimeException("Token already used");
        if (prt.getExpiresAt().isBefore(LocalDateTime.now())) throw new RuntimeException("Token expired");

        User user = prt.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        prt.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(prt);
    }
}
