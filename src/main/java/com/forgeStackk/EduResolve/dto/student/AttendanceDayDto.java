package com.forgeStackk.EduResolve.dto.student;

import java.time.LocalDate;

public record AttendanceDayDto(
        LocalDate date,
        String status,
        String remarks
) {}
