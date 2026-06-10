package com.forgeStackk.EduResolve.dto.student.submission;

import java.time.Instant;
import java.util.List;

public record HomeworkSubmissionDto(
        Long                         submissionId,
        Long                         assignmentId,
        Long                         studentId,
        String                       textCaption,
        String                       status,
        Instant                      submittedAt,
        Instant                      reviewedAt,
        List<SubmissionAttachmentDto> attachments
) {}
