package com.forgeStackk.EduResolve.service.student;

import com.forgeStackk.EduResolve.dto.student.submission.HomeworkSubmissionDto;
import com.forgeStackk.EduResolve.dto.student.submission.SubmissionAttachmentDto;
import com.forgeStackk.EduResolve.entity.student.HomeworkSubmission;
import com.forgeStackk.EduResolve.entity.student.HomeworkSubmissionAttachment;
import com.forgeStackk.EduResolve.repository.student.HomeworkSubmissionAttachmentRepository;
import com.forgeStackk.EduResolve.repository.student.HomeworkSubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomeworkSubmissionService {

    @Value("${app.uploads.root:uploads/teacher-messages}")
    private String uploadRoot;

    private final HomeworkSubmissionRepository submissionRepo;
    private final HomeworkSubmissionAttachmentRepository attachmentRepo;

    @Transactional
    public HomeworkSubmissionDto submit(
            Long studentId,
            Long assignmentId,
            String textCaption,
            MultipartFile voiceNote,
            List<MultipartFile> images,
            List<MultipartFile> files) {

        HomeworkSubmission sub = submissionRepo
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseGet(HomeworkSubmission::new);

        sub.setAssignmentId(assignmentId);
        sub.setStudentId(studentId);
        sub.setTextCaption(textCaption);
        sub.setStatus("SUBMITTED");
        sub.setReviewedAt(null);
        HomeworkSubmission saved = submissionRepo.save(sub);

        List<HomeworkSubmissionAttachment> existing = attachmentRepo.findBySubmissionId(saved.getSubmissionId());
        for (HomeworkSubmissionAttachment old : existing) {
            try { Files.deleteIfExists(Paths.get(old.getFileUrl())); } catch (IOException ignored) {}
        }
        attachmentRepo.deleteAll(existing);

        Path dir = Paths.get(uploadRoot, "submissions", saved.getSubmissionId().toString());
        List<HomeworkSubmissionAttachment> newAtts = new ArrayList<>();
        try {
            if (hasFile(voiceNote)) newAtts.add(saveFile(dir, saved.getSubmissionId(), voiceNote, "VOICE"));
            if (images != null) {
                for (MultipartFile img : images) {
                    if (hasFile(img)) newAtts.add(saveFile(dir, saved.getSubmissionId(), img, "IMAGE"));
                }
            }
            if (files != null) {
                for (MultipartFile f : files) {
                    if (hasFile(f)) newAtts.add(saveFile(dir, saved.getSubmissionId(), f, "DOCUMENT"));
                }
            }
            if (!newAtts.isEmpty()) attachmentRepo.saveAll(newAtts);
        } catch (IOException e) {
            log.error("Submission attachment upload failed: {}", e.getMessage());
            throw new RuntimeException("Attachment upload failed: " + e.getMessage(), e);
        }

        return toDto(saved, newAtts);
    }

    public HomeworkSubmissionDto getStatus(Long studentId, Long assignmentId) {
        HomeworkSubmission sub = submissionRepo
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No submission found"));
        List<HomeworkSubmissionAttachment> atts = attachmentRepo.findBySubmissionId(sub.getSubmissionId());
        return toDto(sub, atts);
    }

    public List<HomeworkSubmissionDto> listForAssignment(Long assignmentId) {
        return submissionRepo.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId)
                .stream()
                .map(sub -> toDto(sub, attachmentRepo.findBySubmissionId(sub.getSubmissionId())))
                .toList();
    }

    @Transactional
    public void markReviewed(Long submissionId) {
        HomeworkSubmission sub = submissionRepo.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        sub.setStatus("REVIEWED");
        sub.setReviewedAt(java.time.Instant.now());
        submissionRepo.save(sub);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private HomeworkSubmissionAttachment saveFile(Path dir, Long submissionId,
                                                   MultipartFile file, String type) throws IOException {
        Files.createDirectories(dir);
        String safeName = System.currentTimeMillis() + "_" +
                (file.getOriginalFilename() != null
                        ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                        : "file");
        Path dest = dir.resolve(safeName).toAbsolutePath();
        file.transferTo(dest);

        HomeworkSubmissionAttachment att = new HomeworkSubmissionAttachment();
        att.setSubmissionId(submissionId);
        att.setFileType(type);
        att.setFileUrl(dest.toString());
        att.setFileName(file.getOriginalFilename());
        att.setFileSizeBytes(file.getSize());
        att.setMimeType(file.getContentType());
        return att;
    }

    private boolean hasFile(MultipartFile f) {
        return f != null && !f.isEmpty();
    }

    private HomeworkSubmissionDto toDto(HomeworkSubmission sub, List<HomeworkSubmissionAttachment> atts) {
        List<SubmissionAttachmentDto> attDtos = atts.stream()
                .map(a -> new SubmissionAttachmentDto(
                        a.getAttachmentId(), a.getFileType(),
                        a.getFileName(), a.getFileSizeBytes() != null ? a.getFileSizeBytes() : 0L,
                        a.getMimeType()))
                .toList();
        return new HomeworkSubmissionDto(
                sub.getSubmissionId(), sub.getAssignmentId(), sub.getStudentId(),
                sub.getTextCaption(), sub.getStatus(), sub.getSubmittedAt(),
                sub.getReviewedAt(), attDtos);
    }
}
