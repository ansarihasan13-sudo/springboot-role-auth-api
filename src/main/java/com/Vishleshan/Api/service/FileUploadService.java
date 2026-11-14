
package com.Vishleshan.Api.service;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.Vishleshan.Api.entity.FileMetadata;
import com.Vishleshan.Api.security.AppUserPrincipal;
import com.Vishleshan.Api.repository.FileMetadataRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileUploadService {

    private final FileValidationService fileValidationService;
    private final ClamAvService clamAvService;
    private final FileMetadataRepository fileMetadataRepository;
    private final Path storagePath;

    public FileUploadService(
            FileValidationService fileValidationService,
            ClamAvService clamAvService,
            FileMetadataRepository fileMetadataRepository,
            Path fileStoragePath) {

        this.fileValidationService = fileValidationService;
        this.clamAvService = clamAvService;
        this.fileMetadataRepository = fileMetadataRepository;
        this.storagePath = fileStoragePath.toAbsolutePath().normalize();
    }

    public FileMetadata upload(
            MultipartFile file,
            AppUserPrincipal principal) throws IOException {

        // 1. Validate file
        String contentType = fileValidationService.validate(file);

        // 2. Antivirus scan
        clamAvService.scan(file);

        // 3. Sanitize original filename
        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null) {
            throw new IllegalArgumentException("Invalid file name");
        }

        originalFilename = originalFilename
                .replace('\\', '/');

        originalFilename = originalFilename.substring(
                originalFilename.lastIndexOf('/') + 1
        );

        // 4. Extract extension
        String extension = originalFilename.substring(
                originalFilename.lastIndexOf('.') + 1
        ).toLowerCase(java.util.Locale.ROOT);

        // 5. Generate unique filenames
        String storageFilename = UUID.randomUUID() + "." + extension;
        String temporaryFilename = UUID.randomUUID() + ".tmp";

        // 6. Create storage directories
        Path stagingDirectory = storagePath.resolve("staging").normalize();
        Path permanentDirectory = storagePath.resolve("files").normalize();

        if (!stagingDirectory.startsWith(storagePath) ||
                !permanentDirectory.startsWith(storagePath)) {
            throw new SecurityException("Invalid storage path");
        }

        Files.createDirectories(stagingDirectory);
        Files.createDirectories(permanentDirectory);

        Path stagingFile = stagingDirectory.resolve(temporaryFilename);
        Path targetFile = permanentDirectory.resolve(storageFilename);

        if (!stagingFile.startsWith(stagingDirectory) ||
                !targetFile.startsWith(permanentDirectory)) {
            throw new SecurityException("Invalid file path");
        }

        try {
            // 7. Write file into staging directory
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, stagingFile);
            }

            // 8. Move staging file to permanent storage
            try {
                Files.move(
                        stagingFile,
                        targetFile,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(stagingFile, targetFile);
            }

            // 9. Prepare metadata
            FileMetadata metadata = new FileMetadata();

            metadata.setOriginalFilename(originalFilename);
            metadata.setStorageFilename(storageFilename);
            metadata.setContentType(contentType);
            metadata.setFileSizeBytes(file.getSize());

            metadata.setUploadedByAccountType(
                    principal.getAccountType()
            );

            metadata.setUploadedByAccountId(
                    principal.getId()
            );

            metadata.setUploadedByEmail(
                    principal.getUsername()
            );

            // 10. Save metadata in PostgreSQL
            return fileMetadataRepository.saveAndFlush(metadata);

        } catch (IOException | RuntimeException exception) {

            // 11. Cleanup temporary and permanent files
            try {
                Files.deleteIfExists(stagingFile);
                Files.deleteIfExists(targetFile);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }

            throw exception;
        }
    }
    public FileDownload getDownload(
            UUID fileId,
            AppUserPrincipal principal) {

        // 1. Find metadata from database
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "File not found"
                ));

        // 2. Check authorization
        boolean isAdmin =
                "ADMIN".equals(principal.getAccountType().name());

        boolean isOwner =
                metadata.getUploadedByAccountId().equals(principal.getId())
                        && metadata.getUploadedByAccountType()
                        == principal.getAccountType();

        if (!isAdmin && !isOwner) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to download this file"
            );
        }

        // 3. Locate file in permanent storage
        Path permanentDirectory =
                storagePath.resolve("files").normalize();

        Path filePath = permanentDirectory
                .resolve(metadata.getStorageFilename())
                .normalize();

        // 4. Prevent path traversal
        if (!filePath.startsWith(permanentDirectory)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid file path"
            );
        }

        // 5. Check file existence
        if (!Files.isRegularFile(filePath)
                || !Files.isReadable(filePath)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Stored file not found"
            );
        }

        // 6. Return metadata and file resource
        Resource resource = new FileSystemResource(filePath);

        return new FileDownload(metadata, resource);
    }
    public record FileDownload(
            FileMetadata metadata,
            Resource resource
    ) {
    }
}
