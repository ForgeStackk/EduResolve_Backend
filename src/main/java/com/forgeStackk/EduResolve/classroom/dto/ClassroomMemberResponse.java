package com.forgeStackk.EduResolve.classroom.dto;

import java.time.Instant;

public record ClassroomMemberResponse(
    Long    userId,
    String  name,
    String  role,
    boolean isOnline,
    Instant lastSeenAt,
    Instant joinedAt
) {}
