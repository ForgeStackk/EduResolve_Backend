package com.forgeStackk.EduResolve.classroom.dto;

public record ClassroomEventPayload(
    String eventType,  // NEW_MESSAGE | MESSAGE_DELETED | REACTION_UPDATED | MESSAGE_PINNED | MEMBER_ONLINE | MEMBER_OFFLINE
    Object payload
) {}
