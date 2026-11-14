
package com.Vishleshan.Api.controller;

import com.Vishleshan.Api.dto.FileUploadResponse;
import com.Vishleshan.Api.entity.FileMetadata;
import com.Vishleshan.Api.security.AppUserPrincipal;
import com.Vishleshan.Api.service.FileUploadService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileUploadService fileUploadService;

    public FileController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    // POST: Upload a file
    @PostMapping(
            value = "/upload",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) throws IOException {

        FileMetadata savedFile =
                fileUploadService.upload(file, principal);

        FileUploadResponse response = new FileUploadResponse(
                savedFile.getId(),
                savedFile.getOriginalFilename(),
                savedFile.getContentType(),
                savedFile.getFileSizeBytes(),
                savedFile.getUploadedByEmail(),
                savedFile.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }
    // GET: Download a file
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable UUID fileId,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        FileUploadService.FileDownload download =
                fileUploadService.getDownload(fileId, principal);

        FileMetadata metadata = download.metadata();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        metadata.getContentType()))
                .contentLength(metadata.getFileSizeBytes())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition
                                .attachment()
                                .filename(
                                        metadata.getOriginalFilename(),
                                        StandardCharsets.UTF_8
                                )
                                .build()
                                .toString()
                )
                .body(download.resource());
    }
}
