package com.forgeStackk.EduResolve.dto.student.submission;

import java.time.Instant;
import java.util.List;

public record DoubtMessageDto(
        Long                         doubtMessageId,
        Long                         threadId,
        Long                         senderId,
        String                       senderRole,
        String                       textBody,
        Instant                      sentAt,
        List<SubmissionAttachmentDto> attachments
) {}
