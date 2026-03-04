package com.ab.authservice.service;

import com.ab.authservice.config.TokenHasher;
import com.ab.authservice.exception.BadRequestException;
import com.ab.authservice.exception.enums.ErrorCode;
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
    private final TokenHasher tokenHasher; // hashes raw token + secret pepper (DB stores only hash)
    private final NotificationService notificationService; // hashes raw token + secret pepper (DB stores only hash)

    @Value("${app.frontend.verify-url}")
    private String verifyUrl; // frontend URL template: ...?token=%s

    //POST /api/v1/auth/verify-email/request -> 202 Accepted
    // If email exists and user is not verified: create token and send email.
    // If email doesn't exist: do nothing
    public void requestVerification(String email, String ip, String userAgent) {
        var userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) return;

        User user = userOpt.get();

        // Already verified => nothing to do
        if (user.isVerified()) return;

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

        // Build verify link for frontend
        String link = verifyUrl.formatted(URLEncoder.encode(rawToken, StandardCharsets.UTF_8));

        // Send email via notification-service (async/decoupled)
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

    // POST /api/v1/auth/verify-email/confirm -> 200 OK (or 400 invalid/expired/used token)
    public void verifyEmail(String rawToken) {
        String tokenHash = tokenHasher.sha256Hex(rawToken);

        EmailVerificationToken evt = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException(ErrorCode.VERIFY_TOKEN_INVALID));

        if (evt.isUsed()) throw new BadRequestException(ErrorCode.VERIFY_TOKEN_USED);
        if (evt.isExpired(LocalDateTime.now())) throw new BadRequestException(ErrorCode.VERIFY_TOKEN_EXPIRED);

        // Mark user verified
        User user = evt.getUser();
        user.setVerified(true);
        userRepository.save(user);

        // Mark user verified
        evt.setUsedAt(LocalDateTime.now());
        tokenRepository.save(evt);
    }
}
