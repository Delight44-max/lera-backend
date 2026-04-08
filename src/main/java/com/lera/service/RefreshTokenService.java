package com.lera.service;

import com.lera.exception.AppException;
import com.lera.model.RefreshToken;
import com.lera.model.User;
import com.lera.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepo;


    @Value("${lera.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    private static final SecureRandom RANDOM = new SecureRandom();


    @Transactional
    public RefreshToken create(User user) {
        String raw = generateSecureToken();
        RefreshToken token = RefreshToken.builder()
            .token(raw)
            .user(user)
            .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
            .revoked(false)
            .build();
        return refreshTokenRepo.save(token);
    }

    @Transactional
    public RefreshToken rotate(String rawToken) {
        RefreshToken existing = refreshTokenRepo.findByToken(rawToken)
            .orElseThrow(() -> AppException.unauthorized("Refresh token not found"));

        if (!existing.isValid()) {

            refreshTokenRepo.revokeAllByUserId(existing.getUser().getId());
            throw AppException.unauthorized(
                existing.isRevoked()
                    ? "Refresh token has been revoked. Please log in again."
                    : "Refresh token has expired. Please log in again."
            );
        }


        existing.setRevoked(true);
        refreshTokenRepo.save(existing);


        return create(existing.getUser());
    }


    @Transactional
    public void revokeAll(String userId) {
        refreshTokenRepo.revokeAllByUserId(userId);
        log.info("All refresh tokens revoked for user:{}", userId);
    }


    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        refreshTokenRepo.deleteExpiredAndRevoked();
        log.info("Purged expired/revoked refresh tokens");
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[64];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
