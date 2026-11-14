
package com.Vishleshan.Api.service;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class FileValidationService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final Map<String, Set<String>> ALLOWED_TYPES = Map.of(
            "application/pdf", Set.of("pdf"),
            "image/jpeg", Set.of("jpg", "jpeg"),
            "image/png", Set.of("png")
    );

    private final Tika tika = new Tika();

    public String validate(MultipartFile file) throws IOException {

        // 1. Check empty file
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "File cannot be empty"
            );
        }

        // 2. Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File size cannot exceed 5 MB"
            );
        }

        // 3. Validate original filename
        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null ||
                originalFilename.isBlank() ||
                originalFilename.length() > 255) {

            throw new IllegalArgumentException(
                    "Invalid file name"
            );
        }

        // 4. Extract extension
        int lastDotIndex = originalFilename.lastIndexOf('.');

        if (lastDotIndex <= 0 ||
                lastDotIndex == originalFilename.length() - 1) {

            throw new IllegalArgumentException(
                    "Invalid file extension"
            );
        }

        String extension = originalFilename
                .substring(lastDotIndex + 1)
                .toLowerCase(Locale.ROOT);

        // 5. Detect actual MIME type using Apache Tika
        String detectedType;

        try (InputStream inputStream = file.getInputStream()) {
            detectedType = tika.detect(inputStream);
        }

        // 6. Validate MIME type and extension combination
        Set<String> allowedExtensions =
                ALLOWED_TYPES.get(detectedType);

        if (allowedExtensions == null ||
                !allowedExtensions.contains(extension)) {

            throw new IllegalArgumentException(
                    "Unsupported file type"
            );
        }

        return detectedType;
    }
}
