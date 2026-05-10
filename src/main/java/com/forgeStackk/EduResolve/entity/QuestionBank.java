package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * Question Bank entity for storing quiz questions extracted from NCERT PDFs.
 * Tagged with difficulty, topic, and chapter for dynamic quiz generation.
 */
@Entity
@Table(name = "question_bank", indexes = {
    @Index(name = "idx_question_bank_chapter", columnList = "chapter_id"),
    @Index(name = "idx_question_bank_difficulty", columnList = "difficulty"),
    @Index(name = "idx_question_bank_topic", columnList = "topic")
})
@Data
@NoArgsConstructor
public class QuestionBank {

    public enum Difficulty { EASY, MEDIUM, HARD }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(name = "option_a")
    private String optionA;

    @Column(name = "option_b")
    private String optionB;

    @Column(name = "option_c")
    private String optionC;

    @Column(name = "option_d")
    private String optionD;

    @Column(name = "correct_option", length = 1)
    private String correctOption; // A, B, C, or D

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty;

    @Column(length = 100)
    private String topic; // e.g., "Sets", "Trigonometry"

    @Column(name = "source_type", length = 50)
    private String sourceType; // e.g., "END_OF_CHAPTER", "EXAMPLE", "EXERCISE"

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
}
