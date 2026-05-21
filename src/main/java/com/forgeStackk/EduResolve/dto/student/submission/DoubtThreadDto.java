package com.forgeStackk.EduResolve.dto.student.submission;

import java.time.Instant;
import java.util.List;

public record DoubtThreadDto(
        Long                  threadId,
        Long                  studentId,
        Long                  teacherId,
        String                teacherName,
        Long                  subjectId,
        String                subjectName,
        Long                  chapterId,
        String                status,
        Instant               createdAt,
        Instant               resolvedAt,
        List<DoubtMessageDto> messages
) {}
