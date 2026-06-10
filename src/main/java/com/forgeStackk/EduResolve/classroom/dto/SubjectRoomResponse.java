package com.forgeStackk.EduResolve.classroom.dto;

import java.time.Instant;

public record SubjectRoomResponse(
    Long    id,
    Long    classroomId,
    Long    subjectId,
    String  subjectName,
    String  name,
    Instant createdAt
) {}
