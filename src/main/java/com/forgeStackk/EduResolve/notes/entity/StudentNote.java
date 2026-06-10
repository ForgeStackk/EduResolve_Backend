package com.forgeStackk.EduResolve.notes.entity;

import com.forgeStackk.EduResolve.util.StringListConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "student_notes", indexes = {
    @Index(name = "idx_notes_student_active",  columnList = "student_id, is_active, created_at DESC"),
    @Index(name = "idx_notes_student_subject", columnList = "student_id, subject_id, is_active"),
    @Index(name = "idx_notes_student_lang",    columnList = "student_id, language, is_active"),
    @Index(name = "idx_notes_student_trash",   columnList = "student_id, is_active, deleted_at")
})
@Data
@NoArgsConstructor
public class StudentNote {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "school_name", nullable = false, length = 200)
    private String schoolName;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "raw_prompt", columnDefinition = "TEXT")
    private String rawPrompt;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    @Column(nullable = false, length = 10)
    private String language = "en";

    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "chapter_ref", length = 255)
    private String chapterRef;

    @Column(name = "source_file_url", length = 500)
    private String sourceFileUrl;

    @Column(name = "source_file_name", length = 255)
    private String sourceFileName;

    @Column(name = "source_page_count")
    private Integer sourcePageCount;

    @Column(name = "ai_model_used", length = 100)
    private String aiModelUsed;

    @Column(name = "is_edited", nullable = false)
    private boolean isEdited = false;

    @Column(name = "is_pinned", nullable = false)
    private boolean isPinned = false;

    @Column(name = "is_shared_to_classroom", nullable = false)
    private boolean isSharedToClassroom = false;

    @Column(name = "shared_classroom_id")
    private Long sharedClassroomId;

    @Convert(converter = StringListConverter.class)
    @Column(name = "tags", columnDefinition = "TEXT")
    private List<String> tags = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "restored_at")
    private Instant restoredAt;

    @Column(name = "is_archived", nullable = false)
    private boolean isArchived = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist  void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate   void onUpdate() { updatedAt = Instant.now(); }
}
