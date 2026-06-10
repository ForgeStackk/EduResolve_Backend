package com.forgeStackk.EduResolve.dto.teacher;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class MarkAttendanceRequest {
    private UUID classId;
    private String classLabel;
    private LocalDate date;
    private List<AttendanceRecordInput> records;
}
