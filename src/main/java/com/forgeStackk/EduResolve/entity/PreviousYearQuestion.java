package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "previous_year_question", indexes = {
    @Index(name = "idx_pyq_chapter",    columnList = "chapter_id"),
    @Index(name = "idx_pyq_difficulty", columnList = "difficulty"),
    @Index(name = "idx_pyq_year",       columnList = "year")
})
@Data
@NoArgsConstructor
public class PreviousYearQuestion {

    public enum Difficulty { EASY, MEDIUM, HARD }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    @Column(name = "topic_id")
    private Long topicId;

    @Column(nullable = false)
    private Integer year;

    @Column(length = 100)
    private String board; // e.g. "CBSE", "ICSE"

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Difficulty difficulty;

    @Column(length = 5, nullable = false)
    private String language = "en";

    /** SHORT, LONG, MCQ, FILL_BLANK */
    @Column(name = "question_type", length = 20)
    private String questionType;

    @Column(name = "marks")
    private Integer marks;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @Column(name = "model_answer", columnDefinition = "TEXT")
    private String modelAnswer;
}
