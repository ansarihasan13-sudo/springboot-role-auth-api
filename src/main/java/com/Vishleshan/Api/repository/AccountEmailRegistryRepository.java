package com.Vishleshan.Api.repository;

import com.Vishleshan.Api.entity.AccountEmailRegistry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountEmailRegistryRepository
        extends JpaRepository<AccountEmailRegistry, String> {

    Optional<AccountEmailRegistry> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<AccountEmailRegistry> findByAccountTypeAndAccountId(
            String accountType,
            UUID accountId
    );
}