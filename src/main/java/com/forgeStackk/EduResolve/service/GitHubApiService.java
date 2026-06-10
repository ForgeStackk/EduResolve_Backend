package com.forgeStackk.EduResolve.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for fetching NCERT PDFs and chapters from local resource folder
 */
@Service
@Slf4j
public class GitHubApiService {

    private static final String NCERT_BASE_PATH = "NCERT";
    private File ncertBaseDir;


    /**
     * Initialize NCERT base directory
     */
    private File getNcertBaseDir() {
        if (ncertBaseDir == null) {
            try {
                Resource resource = new ClassPathResource(NCERT_BASE_PATH);
                ncertBaseDir = resource.getFile();
            } catch (IOException e) {
                log.error("Error initializing NCERT base directory", e);
                ncertBaseDir = null;
            }
        }
        return ncertBaseDir;
    }

    /**
     * Get list of available classes from NCERT folder
     */
    public List<String> getAvailableClasses() {
        try {
            File baseDir = getNcertBaseDir();
            if (baseDir == null || !baseDir.exists()) {
                log.warn("NCERT base directory not found");
                return new ArrayList<>();
            }

            return Arrays.stream(baseDir.listFiles(File::isDirectory))
                .map(File::getName)
                .filter(name -> !name.startsWith("."))
                .map(this::normalizeClassName)
                .sorted()
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching available classes", e);
            return new ArrayList<>();
        }
    }

    /**
     * Normalize class name from folder name (e.g., "NCERT 9th class" -> "9")
     */
    private String normalizeClassName(String folderName) {
        // Extract class number from folder names like "NCERT 9th class", "Class 10", etc.
        if (folderName.matches(".*\\d+.*")) {
            return folderName.replaceAll("\\D+", "").trim();
        }
        return folderName;
    }

    /**
     * Get subjects for a specific class
     */
    public List<String> getSubjectsByClass(String className) {
        try {
            File baseDir = getNcertBaseDir();
            if (baseDir == null || !baseDir.exists()) {
                return new ArrayList<>();
            }

            // Find the actual class directory that contains the class number
            File classDir = Arrays.stream(baseDir.listFiles(File::isDirectory))
                .filter(dir -> dir.getName().contains(className))
                .findFirst()
                .orElse(null);

            if (classDir == null || !classDir.exists()) {
                log.warn("Class directory not found for class: {}", className);
                return new ArrayList<>();
            }

            return Arrays.stream(classDir.listFiles(File::isDirectory))
                .map(File::getName)
                .filter(name -> !name.startsWith("."))
                .map(this::normalizeSubjectName)
                .sorted()
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching subjects for class: {}", className, e);
            return new ArrayList<>();
        }
    }

    /**
     * Normalize subject name from folder name (e.g., "Mathematics book" -> "Mathematics")
     */
    private String normalizeSubjectName(String folderName) {
        String normalized = folderName.replace(" book", "").replace(" Book", "").trim();
        // Handle special cases
        if (normalized.equalsIgnoreCase("Hiindi")) {
            return "Hindi";
        }
        if (normalized.equalsIgnoreCase("PEd")) {
            return "Physical Education";
        }
        return normalized;
    }

    /**
     * Get chapters/books for a specific class and subject
     */
    public List<GitHubBookInfo> getBooks(String className, String subject) {
        try {
            File baseDir = getNcertBaseDir();
            if (baseDir == null || !baseDir.exists()) {
                return new ArrayList<>();
            }

            // Find the actual class directory that contains the class number
            File classDir = Arrays.stream(baseDir.listFiles(File::isDirectory))
                .filter(dir -> dir.getName().contains(className))
                .findFirst()
                .orElse(null);

            if (classDir == null || !classDir.exists()) {
                log.warn("Class directory not found for class: {}", className);
                return new ArrayList<>();
            }

            // Find the actual subject directory with more precise matching
            File subjectDir = Arrays.stream(classDir.listFiles(File::isDirectory))
                .filter(dir -> {
                    String normalizedFolderName = normalizeSubjectName(dir.getName()).toLowerCase();
                    String normalizedSubject = subject.toLowerCase();
                    // Direct match or contains match
                    return normalizedFolderName.equals(normalizedSubject) || 
                           normalizedFolderName.contains(normalizedSubject) ||
                           normalizedSubject.contains(normalizedFolderName);
                })
                .findFirst()
                .orElse(null);

            if (subjectDir == null || !subjectDir.exists()) {
                log.warn("Subject directory not found for class: {}, subject: {}", className, subject);
                return new ArrayList<>();
            }

            // If the subject directory contains subdirectories, each one is a separate book
            // (e.g. class 10 English → "First Flight", "Footprints without feet", ...)
            File[] subdirs = subjectDir.listFiles(File::isDirectory);
            if (subdirs != null && subdirs.length > 0) {
                return Arrays.stream(subdirs)
                    .filter(d -> !d.getName().startsWith("."))
                    .map(dir -> {
                        GitHubBookInfo book = new GitHubBookInfo();
                        book.setBookId(dir.getName());
                        book.setTitle(dir.getName());
                        book.setFilename("");  // empty = folder-type book (chapters inside)
                        book.setPath(dir.getAbsolutePath());
                        return book;
                    })
                    .sorted(java.util.Comparator.comparing(GitHubBookInfo::getTitle))
                    .collect(Collectors.toList());
            }

            // No subdirectories — treat each PDF as a chapter/book directly
            File[] files = subjectDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
            if (files == null) {
                return new ArrayList<>();
            }
            return Arrays.stream(files)
                .map(file -> {
                    GitHubBookInfo book = new GitHubBookInfo();
                    book.setBookId(file.getName().replaceAll("\\.pdf$", ""));
                    book.setTitle(formatTitle(file.getName()));
                    book.setFilename(file.getName());
                    book.setPath(file.getAbsolutePath());
                    return book;
                })
                .sorted((a, b) -> a.getFilename().compareTo(b.getFilename()))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching books for class: {}, subject: {}", className, subject, e);
            return new ArrayList<>();
        }
    }

    /**
     * Get book by ID
     */
    public GitHubBookInfo getBookById(String className, String subject, String bookId) {
        try {
            List<GitHubBookInfo> books = getBooks(className, subject);
            return books.stream()
                .filter(b -> b.getBookId().equals(bookId))
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            log.error("Error getting book by ID: {}", bookId, e);
            return null;
        }
    }

    /**
     * Format filename to readable title
     */
    private String formatTitle(String filename) {
        return filename
            .replaceAll("\\.pdf$", "")
            .replaceAll("_", " ")
            .replaceAll("([a-z])([A-Z])", "$1 $2");
    }

    @Data
    public static class GitHubBookInfo {
        private String bookId;
        private String title;
        private String filename;
        private String path;
    }
}
