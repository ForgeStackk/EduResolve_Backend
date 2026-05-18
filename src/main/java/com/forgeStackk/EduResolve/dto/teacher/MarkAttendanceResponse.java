package com.forgeStackk.EduResolve.dto.teacher;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MarkAttendanceResponse {
    private boolean saved;
    private int count;
}
