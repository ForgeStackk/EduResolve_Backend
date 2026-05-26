package com.forgeStackk.EduResolve.dto.admin.attendance;

import java.time.Instant;

public record AttendancePolicyDto(
        int minAttendancePct,
        int atRiskDropPct,
        boolean autoNotifyParents,
        int autoNotifyThresholdPct,
        int autoNotifyCooldownDays,
        String lastUpdatedBy,
        Instant lastUpdatedAt
) {}
