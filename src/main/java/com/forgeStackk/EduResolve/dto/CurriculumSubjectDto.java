package com.forgeStackk.EduResolve.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumSubjectDto {
    private String name;
    private String icon;
    private String colorHex;
    private List<CurriculumChapterDto> chapters;
}
