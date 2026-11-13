
package com.Vishleshan.Api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileStorageConfig {

    @Value("${app.file.storage-location}")
    private String storageLocation;

    @Bean
    public Path fileStoragePath() {
        return Paths.get(storageLocation)
                .toAbsolutePath()
                .normalize();
    }
}
