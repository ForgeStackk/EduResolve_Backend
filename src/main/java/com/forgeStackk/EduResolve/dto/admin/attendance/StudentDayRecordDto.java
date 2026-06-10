package com.forgeStackk.EduResolve.dto.admin.attendance;

import java.time.LocalDate;

public record StudentDayRecordDto(
        LocalDate date,
        String status,
        String reasonCode,
        String remarks,
        String markedBy
) {}
