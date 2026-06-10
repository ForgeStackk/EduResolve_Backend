package com.forgeStackk.EduResolve.notes.dto;

public record ShareToClassroomRequest(
    Long classroomId,
    Long subjectRoomId  // nullable — null means general room
) {}
