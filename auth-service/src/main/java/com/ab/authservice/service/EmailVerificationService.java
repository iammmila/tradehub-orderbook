package com.ab.authservice.service;

import com.ab.authservice.config.TokenHasher;
import com.ab.authservice.messaging.*;
import com.ab.authservice.model.EmailVerificationToken;
import com.ab.authservice.model.User;
import com.ab.authservice.repository.EmailVerificationTokenRepository;
import com.ab.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final TokenHasher tokenHasher;
    private final NotificationService notificationService;

    @Value("${app.frontend.verify-url}")
    private String verifyUrl; // e.g. http://localhost:3000/verify-email?token=%s

    public void requestVerification(String email, String ip, String userAgent) {
        var userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) return;

        User user = userOpt.get();
        if (Boolean.TRUE.equals(user.isVerified())) return;

        tokenRepository.invalidateAllActiveForUser(user.getId(), LocalDateTime.now());

        String rawToken = UUID.randomUUID().toString() + UUID.randomUUID();
        String tokenHash = tokenHasher.sha256Hex(rawToken);

        EmailVerificationToken evt = EmailVerificationToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .requestIp(ip)
                .userAgent(userAgent)
                .build();

        tokenRepository.save(evt);

        String link = verifyUrl.formatted(URLEncoder.encode(rawToken, StandardCharsets.UTF_8));

        notificationService.send(
                NotificationCommand.builder()
                        .channel(Channel.EMAIL)
                        .to(user.getEmail())
                        .template(Template.EMAIL_VERIFICATION)
                        .subject("Verify your email")
                        .variables(Map.of("verifyLink", link))
                        .locale("en")
                        .build()
        );
    }

    public void verifyEmail(String rawToken) {
        String tokenHash = tokenHasher.sha256Hex(rawToken);

        EmailVerificationToken evt = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (evt.isUsed()) throw new RuntimeException("Token already used");
        if (evt.isExpired(LocalDateTime.now())) throw new RuntimeException("Token expired");

        User user = evt.getUser();
        user.setVerified(true);
        userRepository.save(user);

        evt.setUsedAt(LocalDateTime.now());
        tokenRepository.save(evt);
    }
}
