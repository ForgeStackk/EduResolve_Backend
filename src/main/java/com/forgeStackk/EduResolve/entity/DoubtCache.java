package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Cache of doubts answered by AI (or curated by teachers). Looked up before
 * making any AI call to keep cost minimal.
 *
 * `query_hash` is a normalized SHA-256 of the lowercase, whitespace-collapsed
 * student question - allows O(1) exact-match lookup.
 */
@Entity
@Table(name = "doubt_cache", indexes = {
    @Index(name = "uq_doubt_cache_hash", columnList = "query_hash", unique = true),
    @Index(name = "idx_doubt_cache_lang", columnList = "language")
})
@Data
@NoArgsConstructor
public class DoubtCache {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query_hash", length = 64, nullable = false, unique = true)
    private String queryHash;

    @Column(name = "normalized_query", columnDefinition = "TEXT", nullable = false)
    private String normalizedQuery;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;

    @Column(length = 5, nullable = false)
    private String language = "en";

    @Column(length = 100)
    private String subject;

    /** AI | DB | TEACHER */
    @Column(length = 20)
    private String source;

    @Column(name = "hit_count", nullable = false)
    private Integer hitCount = 0;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "last_hit_at")
    private Instant lastHitAt;

    @PrePersist void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (hitCount == null) hitCount = 0;
    }
}
