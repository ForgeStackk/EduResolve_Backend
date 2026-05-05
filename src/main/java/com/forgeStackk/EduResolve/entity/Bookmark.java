package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Generic bookmark - a student saving a question, content chunk, or PYQ for revision.
 */
@Entity
@Table(name = "bookmark", indexes = {
    @Index(name = "idx_bookmark_student", columnList = "student_id"),
    @Index(name = "idx_bookmark_target",  columnList = "target_type,target_id")
})
@Data
@NoArgsConstructor
public class Bookmark {

    public enum TargetType { CONTENT, QUIZ_QUESTION, PYQ, DOUBT }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /** Optional snapshot for fast list rendering. */
    @Column(length = 255)
    private String label;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
}
