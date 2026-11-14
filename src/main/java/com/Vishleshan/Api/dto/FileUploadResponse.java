
package com.Vishleshan.Api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class FileUploadResponse {

    private UUID id;
    private String originalFilename;
    private String contentType;
    private Long fileSizeBytes;
    private String uploadedByEmail;
    private LocalDateTime createdAt;

    public FileUploadResponse(
            UUID id,
            String originalFilename,
            String contentType,
            Long fileSizeBytes,
            String uploadedByEmail,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.uploadedByEmail = uploadedByEmail;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getUploadedByEmail() {
        return uploadedByEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
