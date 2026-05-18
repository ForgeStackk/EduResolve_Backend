package com.forgeStackk.EduResolve.dto.teacher;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ClassResponse {
    private UUID classId;
    private String className;
    private String section;
    private boolean isClassTeacher;
}
