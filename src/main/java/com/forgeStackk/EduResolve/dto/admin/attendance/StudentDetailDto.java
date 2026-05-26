package com.forgeStackk.EduResolve.dto.admin.attendance;

import java.util.List;
import java.util.Map;

public record StudentDetailDto(
        Long studentId,
        String name,
        String className,
        int totalDays,
        int present,
        int absent,
        int late,
        int excused,
        Double percentage,
        List<StudentDayRecordDto> records,
        Map<String, Long> reasonsBreakdown,
        List<LeaveApplicationSummaryDto> leaveApplications,
        List<AuditEntryDto> auditHistory
) {}
