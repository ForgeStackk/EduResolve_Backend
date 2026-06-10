package com.forgeStackk.EduResolve.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AskDoubtRequest {
    private Long studentId;
    @NotBlank(message = "query is required")
    private String query;
    @NotBlank(message = "subject is required")
    private String subject;
}
