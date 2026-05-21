package com.forgeStackk.EduResolve.controller.student;

import com.forgeStackk.EduResolve.dto.student.submission.DoubtMessageDto;
import com.forgeStackk.EduResolve.dto.student.submission.DoubtThreadDto;
import com.forgeStackk.EduResolve.entity.student.DoubtMessageAttachment;
import com.forgeStackk.EduResolve.repository.student.DoubtMessageAttachmentRepository;
import com.forgeStackk.EduResolve.security.StudentPortalAuthHelper;
import com.forgeStackk.EduResolve.service.student.DoubtService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student-portal/doubts")
@RequiredArgsConstructor
public class StudentDoubtController {

    private final DoubtService                       doubtService;
    private final StudentPortalAuthHelper            authHelper;
    private final DoubtMessageAttachmentRepository   attRepo;

    /** Open a new doubt thread to the subject teacher.
     *  classId is optional — if omitted the backend resolves it from the student's own class. */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<DoubtThreadDto> openThread(
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long chapterId,
            @RequestParam(required = false) String textBody,
            @RequestPart(required = false) MultipartFile voiceNote,
            @RequestPart(required = false) List<MultipartFile> images) {

        Long studentId = authHelper.resolveUserLoginId();
        return ResponseEntity.ok(
                doubtService.openThread(studentId, classId, subjectId, chapterId,
                        textBody, voiceNote, images));
    }

    /** Reply to an existing thread (student side). */
    @PostMapping("/{threadId}/reply")
    public ResponseEntity<DoubtMessageDto> reply(
            @PathVariable Long threadId,
            @RequestParam(required = false) String textBody,
            @RequestPart(required = false) MultipartFile voiceNote,
            @RequestPart(required = false) List<MultipartFile> images) {

        Long studentId = authHelper.resolveUserLoginId();
        return ResponseEntity.ok(
                doubtService.reply(threadId, studentId, "STUDENT", textBody, voiceNote, images));
    }

    /** Student marks the doubt as resolved. */
    @PatchMapping("/{threadId}/resolve")
    public ResponseEntity<Void> resolve(@PathVariable Long threadId) {
        Long studentId = authHelper.resolveUserLoginId();
        doubtService.resolve(threadId, studentId);
        return ResponseEntity.noContent().build();
    }

    /** Get all doubt threads for the current student. */
    @GetMapping
    public ResponseEntity<List<DoubtThreadDto>> list() {
        Long studentId = authHelper.resolveUserLoginId();
        return ResponseEntity.ok(doubtService.listByStudent(studentId));
    }

    /** Get one thread with all messages. */
    @GetMapping("/{threadId}")
    public ResponseEntity<DoubtThreadDto> getThread(@PathVariable Long threadId) {
        return ResponseEntity.ok(doubtService.getThread(threadId));
    }

    /** Serve a doubt message attachment (image or voice note). */
    @GetMapping("/attachments/{attachmentId}/content")
    public ResponseEntity<Resource> getAttachment(@PathVariable Long attachmentId) {
        DoubtMessageAttachment att = attRepo.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));

        Path path = Paths.get(att.getFileUrl());
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found on disk");
        }

        String mimeType = (att.getMimeType() != null && !att.getMimeType().isBlank())
                ? att.getMimeType() : "application/octet-stream";
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
