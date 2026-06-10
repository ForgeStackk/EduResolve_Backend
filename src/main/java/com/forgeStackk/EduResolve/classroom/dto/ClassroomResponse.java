package com.forgeStackk.EduResolve.classroom.dto;

public record ClassroomResponse(
    Long   id,
    String name,
    String classLabel,
    String schoolName,
    long   memberCount,
    long   onlineCount
) {}
