
package com.Vishleshan.Api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileStorageConfig {

    private final String storageLocation;

    public FileStorageConfig(
            @Value("${app.file.storage-location}") String storageLocation) {

        if (storageLocation == null || storageLocation.isBlank()) {
            throw new IllegalArgumentException(
                    "File storage location cannot be empty"
            );
        }

        this.storageLocation = storageLocation;
    }

    @Bean
    public Path fileStoragePath() {

        Path storagePath = Paths.get(storageLocation)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(storagePath);

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not initialize file storage directory",
                    exception
            );
        }

        if (!Files.isDirectory(storagePath)) {
            throw new IllegalStateException(
                    "Storage path is not a directory"
            );
        }

        if (!Files.isWritable(storagePath)) {
            throw new IllegalStateException(
                    "Storage directory is not writable"
            );
        }

        return storagePath;
    }
}
