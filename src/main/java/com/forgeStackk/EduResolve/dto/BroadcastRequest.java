package com.forgeStackk.EduResolve.dto;

import java.util.UUID;

public record BroadcastRequest(
        String channels,
        UUID classId,
        boolean targetStudents,
        boolean targetParents,
        String message,
        boolean isEmergency,
        String sentByName
) {}
