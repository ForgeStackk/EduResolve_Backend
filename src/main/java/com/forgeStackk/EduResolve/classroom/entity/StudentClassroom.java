package com.forgeStackk.EduResolve.classroom.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "student_classrooms",
    uniqueConstraints = @UniqueConstraint(name = "uq_student_classroom", columnNames = {"class_label", "school_name"}),
    indexes = @Index(name = "idx_student_classrooms_label", columnList = "class_label, school_name"))
@Data
@NoArgsConstructor
public class StudentClassroom {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** References classroom.seq_id in the teacher portal (no DB-level FK — Option A). */
    @Column(name = "classroom_seq_id")
    private Long classroomSeqId;

    @Column(name = "school_name", nullable = false, length = 200)
    private String schoolName;

    /** Mirrors user_login.class_name, e.g. "9A", "10B". Used for fast student lookup. */
    @Column(name = "class_label", nullable = false, length = 50)
    private String classLabel;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }
}
