package com.forgeStackk.EduResolve.dto.teacher;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class GenerateReportRequest {
    private UUID classId;
    private Integer month;
    private Integer year;
}
