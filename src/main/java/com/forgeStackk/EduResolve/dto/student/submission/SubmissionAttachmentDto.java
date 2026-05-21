package com.forgeStackk.EduResolve.dto.student.submission;

public record SubmissionAttachmentDto(
        Long   attachmentId,
        String fileType,
        String fileName,
        long   fileSizeBytes,
        String mimeType
) {}
