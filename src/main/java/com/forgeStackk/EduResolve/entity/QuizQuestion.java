package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quiz_question", indexes = {
    @Index(name = "idx_quiz_chapter_diff", columnList = "chapter_id,difficulty"),
    @Index(name = "idx_quiz_topic",        columnList = "topic_id"),
    @Index(name = "idx_quiz_lang",         columnList = "language")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestion {

    public enum Difficulty { EASY, MEDIUM, HARD }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String subject;

    /** Free-text chapter name (legacy). Prefer chapterId for new content. */
    @Column(length = 255)
    private String chapter;

    @Column(name = "chapter_id")
    private Long chapterId;

    @Column(name = "topic_id")
    private Long topicId;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Difficulty difficulty = Difficulty.MEDIUM;

    @Column(length = 5, nullable = false)
    private String language = "en";

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    /** JSON array string: [{"id":"o1","text":"..."}] */
    @Column(columnDefinition = "TEXT")
    private String optionsJson;

    @Column(name = "correct_option_id", length = 50)
    private String correctOptionId;

    @Column(columnDefinition = "TEXT")
    private String explanation;
}
