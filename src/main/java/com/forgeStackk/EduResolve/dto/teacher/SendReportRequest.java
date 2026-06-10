package com.forgeStackk.EduResolve.dto.teacher;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SendReportRequest {
    /** Accepted values: "STUDENT_PORTAL", "PARENT_PORTAL" */
    private List<String> sendTo;
}
