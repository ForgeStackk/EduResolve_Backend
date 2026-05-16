package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.dto.CurriculumChapterDto;
import com.forgeStackk.EduResolve.dto.CurriculumSubjectDto;
import com.forgeStackk.EduResolve.entity.NcertBook;
import com.forgeStackk.EduResolve.repository.NcertBookRepository;
import com.forgeStackk.EduResolve.service.GitHubApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Returns the full curriculum (subjects + chapters per subject) for a given class,
 * derived entirely from the local NCERT resource folder and the ncert_book table
 * that is synced from that folder at startup.
 *
 * GET /api/curriculum/{classGrade}
 *   classGrade can be "9", "10", or a full class string like "10A" (letters are stripped).
 */
@RestController
@RequestMapping("/api/curriculum")
@CrossOrigin(origins = "*")
public class CurriculumController {

    @Autowired
    private GitHubApiService gitHubApiService;

    @Autowired
    private NcertBookRepository ncertBookRepository;

    private static final Map<String, String> SUBJECT_ICONS = new LinkedHashMap<>() {{
        put("Mathematics",        "functions");
        put("Science",            "science");
        put("English",            "menu_book");
        put("Hindi",              "translate");
        put("Sanskrit",           "history_edu");
        put("Physical Education", "sports");
        put("Social Science",     "public");
    }};

    private static final Map<String, String> SUBJECT_COLORS = new LinkedHashMap<>() {{
        put("Mathematics",        "#8b5cf6");
        put("Science",            "#10b981");
        put("English",            "#ec4899");
        put("Hindi",              "#f59e0b");
        put("Sanskrit",           "#3b82f6");
        put("Physical Education", "#ef4444");
        put("Social Science",     "#22d3ee");
    }};

    @GetMapping("/{classGrade}")
    public ResponseEntity<List<CurriculumSubjectDto>> getCurriculum(@PathVariable String classGrade) {
        // Accept "10A", "10B", "9" etc — strip everything that isn't a digit.
        String grade = classGrade.replaceAll("[^0-9]", "").trim();
        if (grade.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Primary source: filesystem scan via GitHubApiService
        List<String> subjectNames = gitHubApiService.getSubjectsByClass(grade);

        // Fallback: subjects already stored in ncert_book table (handles the case
        // where the NCERT folder isn't on the classpath at the time of the call)
        if (subjectNames.isEmpty()) {
            subjectNames = ncertBookRepository.findDistinctSubjectsByClassGrade(grade);
        }

        List<CurriculumSubjectDto> curriculum = subjectNames.stream()
            .map(subjectName -> buildSubject(grade, subjectName))
            .collect(Collectors.toList());

        return ResponseEntity.ok(curriculum);
    }

    private CurriculumSubjectDto buildSubject(String grade, String subjectName) {
        List<NcertBook> books = ncertBookRepository.findByClassGradeAndSubject(grade, subjectName);

        List<CurriculumChapterDto> chapters = books.stream()
            .map(book -> {
                int num = extractChapterNumber(book.getPdfFilename());
                return new CurriculumChapterDto(book.getId(), num, "Chapter " + num, book.getPdfFilename());
            })
            .sorted(Comparator.comparingInt(CurriculumChapterDto::getChapterNumber))
            .collect(Collectors.toList());

        return new CurriculumSubjectDto(
            subjectName,
            SUBJECT_ICONS.getOrDefault(subjectName, "school"),
            SUBJECT_COLORS.getOrDefault(subjectName, "#dc2626"),
            chapters
        );
    }

    /**
     * Extracts the chapter number from an NCERT PDF filename.
     *
     * NCERT filenames follow the pattern: [subject-code][class-digit][CC].pdf
     * where CC is a zero-padded chapter number (01, 02, ..., 13).
     * Examples: iesc101.pdf -> 1, iemh108.pdf -> 8, iebe102.pdf -> 2
     */
    private int extractChapterNumber(String filename) {
        if (filename == null || filename.isBlank()) return 1;
        String stem = filename.replaceAll("(?i)\\.pdf$", "");
        // Pattern: ends in exactly 3 digits like "101", "102", "113"
        if (stem.matches(".*\\d{3}$")) {
            // Last 2 digits = chapter number (e.g. "101" -> 01 -> 1)
            return Integer.parseInt(stem.substring(stem.length() - 2));
        }
        // Fallback: grab all trailing digits
        String trailing = stem.replaceAll(".*?(\\d+)$", "$1");
        try {
            int n = Integer.parseInt(trailing);
            return n > 99 ? n % 100 : n;
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
