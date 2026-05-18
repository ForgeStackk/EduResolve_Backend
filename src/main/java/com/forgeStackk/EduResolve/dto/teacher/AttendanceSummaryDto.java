package com.forgeStackk.EduResolve.dto.teacher;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryDto {
    private int totalWorkingDays;
    private List<StudentAttendanceSummary> studentWiseSummary;
}
