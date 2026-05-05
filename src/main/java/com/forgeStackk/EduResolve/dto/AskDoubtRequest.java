package com.forgeStackk.EduResolve.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AskDoubtRequest {
    private Long studentId;
    private String query;
    private String subject;
}
