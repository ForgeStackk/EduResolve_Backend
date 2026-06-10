package com.forgeStackk.EduResolve.dto.admin.attendance;

public record StudentAttendanceSummaryDto(
        Long studentId,
        String name,
        String rollNumber,
        int totalDays,
        int present,
        int absent,
        int late,
        int excused,
        Double percentage,
        boolean atRisk,
        String trend,
        String dominantAbsenceReason
) {}
