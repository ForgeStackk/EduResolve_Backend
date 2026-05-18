package com.forgeStackk.EduResolve.dto.teacher;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class GenerateReportResponse {
    private UUID reportId;
    private String reportFileUrl;
    private AttendanceSummaryDto summary;
}
