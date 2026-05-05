package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Per-topic accuracy + time tracking, updated on every quiz attempt.
 * Powers the "weak topics" recommendation engine (no AI needed).
 */
@Entity
@Table(name = "student_performance", indexes = {
    @Index(name = "idx_perf_student_topic", columnList = "student_id,topic_id", unique = true),
    @Index(name = "idx_perf_student_acc",   columnList = "student_id,accuracy")
})
@Data
@NoArgsConstructor
public class StudentPerformance {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "questions_attempted", nullable = false)
    private Integer questionsAttempted = 0;

    @Column(name = "questions_correct", nullable = false)
    private Integer questionsCorrect = 0;

    /** Cached value: questions_correct / questions_attempted * 100 (0-100). */
    @Column(nullable = false)
    private Double accuracy = 0.0;

    @Column(name = "time_spent_seconds")
    private Long timeSpentSeconds = 0L;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;
}
