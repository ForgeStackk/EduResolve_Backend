package com.forgeStackk.EduResolve.dto.teacher;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class StudentWithParentResponse {
    private UUID studentId;
    private String fullName;
    private String rollNumber;
    private String parentName;
    private String parentPhone;
}
