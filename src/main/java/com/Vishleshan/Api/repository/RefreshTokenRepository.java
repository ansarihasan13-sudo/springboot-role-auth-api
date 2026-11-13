
package com.Vishleshan.Api.repository;

import com.Vishleshan.Api.entity.AccountType;
import com.Vishleshan.Api.entity.RefreshToken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, UUID> {

    // Find token by its SHA-256 hash
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // Lock token during rotation to prevent concurrent refresh
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM RefreshToken t WHERE t.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    void deleteAllByAccountIdAndAccountType(
            UUID accountId,
            AccountType accountType
    );

    // Find all tokens belonging to one rotation family
    List<RefreshToken> findAllByTokenFamilyId(UUID tokenFamilyId);

    // Find all tokens for an account
    List<RefreshToken> findAllByAccountIdAndAccountType(
            UUID accountId,
            AccountType accountType
    );
}
