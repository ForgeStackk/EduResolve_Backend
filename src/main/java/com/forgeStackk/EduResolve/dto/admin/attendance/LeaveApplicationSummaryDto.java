package com.forgeStackk.EduResolve.dto.admin.attendance;

import java.time.LocalDate;

public record LeaveApplicationSummaryDto(
        Long id,
        LocalDate fromDate,
        LocalDate toDate,
        String reason,
        String status
) {}
