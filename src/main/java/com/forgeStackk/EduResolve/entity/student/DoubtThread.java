package com.forgeStackk.EduResolve.entity.student;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "doubt_thread")
@Data
@NoArgsConstructor
public class DoubtThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "thread_id", updatable = false, nullable = false)
    private Long threadId;

    /** References user_login.id of the student. */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /** References user_login.id of the teacher. */
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "chapter_id")
    private Long chapterId;

    /** OPEN or RESOLVED */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "OPEN";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
