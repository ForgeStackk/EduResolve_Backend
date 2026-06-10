package com.forgeStackk.EduResolve.dto.student;

import com.forgeStackk.EduResolve.enums.MessageCategory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StudentInboxItemDto(
        UUID inboxId,
        UUID messageId,
        Long msgNum,
        String senderName,
        MessageCategory category,
        Long targetSubjectId,
        String subjectName,
        String textBody,
        String contentType,
        Instant sentAt,
        boolean isHomework,
        LocalDate homeworkDueDate,
        String readStatus,
        List<AttachmentInfo> attachments
) {
    public record AttachmentInfo(
            UUID attachmentId,
            String fileType,
            String fileName,
            long fileSizeBytes,
            String mimeType
    ) {}
}
