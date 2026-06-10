package com.forgeStackk.EduResolve.dto.teacher;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ClassResponse {
    private UUID classId;
    private String className;
    private String section;
    // Explicit name prevents Jackson from stripping "is" prefix (isClassTeacher → classTeacher)
    @JsonProperty("isClassTeacher")
    private boolean isClassTeacher;
}
