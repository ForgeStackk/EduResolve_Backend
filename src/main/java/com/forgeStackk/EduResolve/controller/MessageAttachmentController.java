package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.teacher.MessageAttachment;
import com.forgeStackk.EduResolve.repository.teacher.MessageAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages/attachments")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MessageAttachmentController {

    private final MessageAttachmentRepository attachmentRepo;

    @GetMapping("/{attachmentId}/content")
    public ResponseEntity<Resource> getContent(@PathVariable UUID attachmentId) {
        MessageAttachment att = attachmentRepo.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));

        Path path = Paths.get(att.getFileUrl());
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found on disk");
        }

        String mimeType = (att.getMimeType() != null && !att.getMimeType().isBlank())
                ? att.getMimeType() : "application/octet-stream";

        // Images and audio render inline; documents trigger download
        boolean isInline = mimeType.startsWith("image/") || mimeType.startsWith("audio/");
        String safeFileName = att.getFileName() != null
                ? att.getFileName().replaceAll("[^a-zA-Z0-9._\\-() ]", "_") : "file";
        String disposition = (isInline ? "inline" : "attachment") + "; filename=\"" + safeFileName + "\"";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=86400")
                .body(new FileSystemResource(path));
    }
}
