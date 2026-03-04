package com.ab.authservice.repository;

import com.ab.authservice.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
                update EmailVerificationToken t
                   set t.usedAt = :now
                 where t.user.id = :userId
                   and t.usedAt is null
                   and t.expiresAt > :now
            """)
    int invalidateAllActiveForUser(Long userId, LocalDateTime now);
}
