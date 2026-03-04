package com.ab.authservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification_tokens",
        indexes = {
                @Index(name = "idx_evt_token_hash", columnList = "tokenHash", unique = true),
                @Index(name = "idx_evt_user_id", columnList = "user_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // many tokens over time for one user
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // when user clicked verify link
    private LocalDateTime usedAt;

    private String requestIp;

    @Column(length = 500)
    private String userAgent;

    public boolean isExpired(LocalDateTime now) {
        return expiresAt != null && expiresAt.isBefore(now);
    }

    public boolean isUsed() {
        return usedAt != null;
    }
}
