
package com.Vishleshan.Api.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "files")
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "storage_filename", nullable = false, unique = true, length = 100)
    private String storageFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "uploaded_by_account_type", nullable = false, length = 20)
    private AccountType uploadedByAccountType;

    @Column(name = "uploaded_by_account_id", nullable = false)
    private UUID uploadedByAccountId;

    @Column(name = "uploaded_by_email", nullable = false, length = 150)
    private String uploadedByEmail;

    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    public FileMetadata() {
    }

    public UUID getId() {
        return id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStorageFilename() {
        return storageFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public AccountType getUploadedByAccountType() {
        return uploadedByAccountType;
    }

    public UUID getUploadedByAccountId() {
        return uploadedByAccountId;
    }

    public String getUploadedByEmail() {
        return uploadedByEmail;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public void setStorageFilename(String storageFilename) {
        this.storageFilename = storageFilename;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public void setUploadedByAccountType(AccountType uploadedByAccountType) {
        this.uploadedByAccountType = uploadedByAccountType;
    }

    public void setUploadedByAccountId(UUID uploadedByAccountId) {
        this.uploadedByAccountId = uploadedByAccountId;
    }

    public void setUploadedByEmail(String uploadedByEmail) {
        this.uploadedByEmail = uploadedByEmail;
    }
}
