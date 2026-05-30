package com.forgeStackk.EduResolve.notes.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "pdf_extraction_jobs",
    indexes = @Index(name = "idx_pdf_jobs_student", columnList = "student_id, status, created_at DESC"))
@Data
@NoArgsConstructor
public class PdfExtractionJob {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** PROCESSING | COMPLETED | FAILED */
    @Column(nullable = false, length = 20)
    private String status = "PROCESSING";

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "character_count")
    private Integer characterCount;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }
}
