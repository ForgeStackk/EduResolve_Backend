package com.forgeStackk.EduResolve.dto.admin.attendance;

import java.time.LocalDate;

public record HeatmapDayDto(
        LocalDate date,
        int presentCount,
        int totalStudents,
        Double percentage
) {}
