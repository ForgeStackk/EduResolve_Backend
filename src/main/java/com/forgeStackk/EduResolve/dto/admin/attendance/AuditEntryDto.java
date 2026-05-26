package com.forgeStackk.EduResolve.dto.admin.attendance;

import java.time.Instant;

public record AuditEntryDto(
        Long id,
        Instant changedAt,
        Long changedByUserId,
        String oldStatus,
        String newStatus,
        String oldReasonCode,
        String newReasonCode,
        String note
) {}
