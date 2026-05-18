package com.forgeStackk.EduResolve.dto.teacher;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceSummary {
    private UUID studentId;
    private String fullName;
    private int present;
    private int absent;
    private int late;
    private int halfDay;
    private int holiday;
}
