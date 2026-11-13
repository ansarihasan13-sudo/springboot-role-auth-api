package com.Vishleshan.Api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "account_email_registry")
public class AccountEmailRegistry {

    @Id
    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    public AccountEmailRegistry() {
    }

    public AccountEmailRegistry(
            String email,
            String accountType,
            UUID accountId) {

        this.email = email;
        this.accountType = accountType;
        this.accountId = accountId;
    }

    public String getEmail() {
        return email;
    }

    public String getAccountType() {
        return accountType;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }
}