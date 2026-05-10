package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * NCERT Book entity representing a textbook stored on GitHub.
 * Links to a class and subject, with metadata and GitHub URL for the PDF.
 */
@Entity
@Table(name = "ncert_book", indexes = {
    @Index(name = "idx_ncert_book_class", columnList = "class_grade"),
    @Index(name = "idx_ncert_book_subject", columnList = "subject")
})
@Data
@NoArgsConstructor
public class NcertBook {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_grade", nullable = false, length = 10)
    private String classGrade; // e.g., "9", "10", "11", "12"

    @Column(nullable = false, length = 100)
    private String subject; // e.g., "Mathematics", "Physics", "Chemistry", "Biology"

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "github_url", columnDefinition = "TEXT")
    private String githubUrl; // GitHub raw URL for the PDF

    @Column(name = "github_repo", length = 255)
    private String githubRepo; // GitHub repository name

    @Column(name = "github_path", columnDefinition = "TEXT")
    private String githubPath; // Path within GitHub repository

    @Column(name = "pdf_filename", length = 255)
    private String pdfFilename;

    @Column(name = "total_pages")
    private Integer totalPages;

    @Column(name = "uploaded_at", updatable = false)
    private Instant uploadedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (uploadedAt == null) uploadedAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
