package com.forgeStackk.EduResolve.dto.teacher;

import com.forgeStackk.EduResolve.enums.AttendanceStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class AttendanceRecordInput {
    private UUID studentId;
    private AttendanceStatus status;
    private String remarks;
}
