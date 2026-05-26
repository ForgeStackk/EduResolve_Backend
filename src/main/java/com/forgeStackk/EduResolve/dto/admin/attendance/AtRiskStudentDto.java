package com.forgeStackk.EduResolve.dto.admin.attendance;

import java.time.Instant;
import java.time.LocalDate;

public record AtRiskStudentDto(
        Long studentId,
        String name,
        String className,
        String rollNumber,
        Double currentPct,
        Double previousPct,
        Double drop,
        String severity,
        LocalDate lastAbsentDate,
        String dominantReason,
        Instant lastNotifiedAt,
        Long daysSinceNotified
) {}
