package com.forgeStackk.EduResolve.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumChapterDto {
    private Long bookId;
    private int chapterNumber;
    private String name;
    private String pdfFilename;
}
