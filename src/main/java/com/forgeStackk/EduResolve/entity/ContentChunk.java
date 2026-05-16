package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Granular piece of learning content extracted from PDFs or curated by admins.
 * One topic can have many chunks (summary, full explanation, examples, PYQ-style notes).
 */
@Entity
@Table(name = "content_chunk", indexes = {
    @Index(name = "idx_content_chunk_topic", columnList = "topic_id"),
    @Index(name = "idx_content_chunk_lang",  columnList = "language")
})
@Data
@NoArgsConstructor
public class ContentChunk {

    public enum ChunkType { SUMMARY, EXPLANATION, EXAMPLE, IMPORTANT_QA, FLASHCARD, ONE_PAGE_NOTE, DIAGRAM }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "chunk_type", nullable = false, length = 20)
    private ChunkType chunkType;

    /** ISO 639-1 (en, hi). */
    @Column(length = 5, nullable = false)
    private String language = "en";

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    /** Reading order within a topic. */
    @Column(name = "order_index")
    private Integer orderIndex;

    /** Source PDF filename or admin user. */
    @Column(length = 255)
    private String source;

    /** URL to diagram or image for this content chunk. */
    @Column(length = 500)
    private String imageUrl;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
}
