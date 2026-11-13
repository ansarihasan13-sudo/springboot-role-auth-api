
package com.Vishleshan.Api.service;

import com.Vishleshan.Api.entity.AccountType;
import com.Vishleshan.Api.entity.RefreshToken;
import com.Vishleshan.Api.repository.RefreshTokenRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    // Generate secure random token
    private String generateToken() {

        byte[] randomBytes = new byte[32];

        SECURE_RANDOM.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    // SHA-256 hashing
    private String hashToken(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                    token.getBytes(
                            java.nio.charset.StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to hash refresh token", e);
        }
    }

    // Create first refresh token for ADMIN, EMPLOYEE or USER
    @Transactional
    public String createRefreshToken(
            UUID accountId,
            AccountType accountType) {

        UUID familyId = UUID.randomUUID();

        return createTokenForFamily(
                accountId,
                accountType,
                familyId
        );
    }

    // Create token inside an existing family
    private String createTokenForFamily(
            UUID accountId,
            AccountType accountType,
            UUID familyId) {

        String rawToken = generateToken();

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setAccountId(accountId);
        refreshToken.setAccountType(accountType);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setTokenFamilyId(familyId);

        refreshToken.setExpiresAt(
                Instant.now().plusMillis(refreshTokenExpiration)
        );

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    // Validate and rotate refresh token
    @Transactional(noRollbackFor = RefreshTokenReuseException.class)
    public RotationResult rotateRefreshToken(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Refresh token is required");
        }

        String tokenHash = hashToken(rawToken);

        RefreshToken oldToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid refresh token"));

        // Detect reuse of revoked token
        if (oldToken.getRevokedAt() != null) {

            revokeTokenFamily(oldToken.getTokenFamilyId());

            throw new RefreshTokenReuseException(
                    "Refresh token reuse detected. Sessions revoked.");
        }

        Instant now = Instant.now();

        // Check expiry
        if (!oldToken.getExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException(
                    "Refresh token expired");
        }

        // Preserve account identity during rotation
        UUID accountId = oldToken.getAccountId();
        AccountType accountType = oldToken.getAccountType();

        // Generate replacement token in same family
        String newRawToken = createTokenForFamily(
                accountId,
                accountType,
                oldToken.getTokenFamilyId()
        );

        // Find newly created token
        RefreshToken newToken = refreshTokenRepository
                .findByTokenHash(hashToken(newRawToken))
                .orElseThrow(() ->
                        new IllegalStateException(
                                "New refresh token not found"));

        // Revoke old token and link replacement
        oldToken.setRevokedAt(now);
        oldToken.setReplacedBy(newToken.getId());

        refreshTokenRepository.save(oldToken);

        return new RotationResult(
                newRawToken,
                accountId,
                accountType
        );
    }

    // Single-device logout
    @Transactional
    public void revokeRefreshToken(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        String tokenHash = hashToken(rawToken);

        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> {

                    if (token.getRevokedAt() == null) {
                        token.setRevokedAt(Instant.now());
                        refreshTokenRepository.save(token);
                    }
                });
    }

    // Revoke complete token family
    private void revokeTokenFamily(UUID familyId) {

        List<RefreshToken> tokens =
                refreshTokenRepository
                        .findAllByTokenFamilyId(familyId);

        Instant now = Instant.now();

        for (RefreshToken token : tokens) {

            if (token.getRevokedAt() == null) {
                token.setRevokedAt(now);
            }
        }

        refreshTokenRepository.saveAll(tokens);
    }

    // Logout all devices for ADMIN, EMPLOYEE or USER
    @Transactional
    public void revokeAllAccountTokens(
            UUID accountId,
            AccountType accountType) {

        List<RefreshToken> tokens =
                refreshTokenRepository
                        .findAllByAccountIdAndAccountType(
                                accountId,
                                accountType
                        );

        Instant now = Instant.now();

        for (RefreshToken token : tokens) {

            if (token.getRevokedAt() == null) {
                token.setRevokedAt(now);
            }
        }

        refreshTokenRepository.saveAll(tokens);
    }

    // Result returned after successful rotation
    public record RotationResult(
            String refreshToken,
            UUID accountId,
            AccountType accountType
    ) {
    }

    // Custom exception for token reuse
    public static class RefreshTokenReuseException
            extends RuntimeException {

        public RefreshTokenReuseException(String message) {
            super(message);
        }
    }
}
