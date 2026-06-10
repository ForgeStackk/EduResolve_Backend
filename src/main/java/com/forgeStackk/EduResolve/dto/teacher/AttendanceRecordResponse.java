package com.forgeStackk.EduResolve.dto.teacher;

import com.forgeStackk.EduResolve.enums.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class AttendanceRecordResponse {
    private UUID studentId;
    private String fullName;
    private String rollNumber;
    private AttendanceStatus status;
    private String remarks;
}
