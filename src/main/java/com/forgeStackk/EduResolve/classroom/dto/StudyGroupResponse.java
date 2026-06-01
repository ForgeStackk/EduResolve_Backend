package com.forgeStackk.EduResolve.classroom.dto;

import java.time.Instant;
import java.util.List;

public record StudyGroupResponse(
    Long             id,
    Long             classroomId,
    String           name,
    String           description,
    Long             ownerUserId,
    String           ownerName,
    boolean          isOwner,
    List<GroupMemberDto> members,
    Instant          createdAt
) {}
