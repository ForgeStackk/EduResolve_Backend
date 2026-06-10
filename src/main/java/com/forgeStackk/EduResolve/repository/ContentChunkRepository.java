package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.ContentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContentChunkRepository extends JpaRepository<ContentChunk, Long> {

    List<ContentChunk> findByTopicIdAndLanguageOrderByOrderIndexAscIdAsc(Long topicId, String language);

    List<ContentChunk> findByTopicIdAndChunkTypeAndLanguage(
        Long topicId, ContentChunk.ChunkType chunkType, String language);

    /**
     * Postgres full-text search on body. Falls back to ILIKE when FTS index
     * is missing (still works, just slower).
     */
    @Query(value = """
        SELECT * FROM content_chunk
        WHERE language = :lang
          AND (
            to_tsvector('simple', coalesce(title,'') || ' ' || body)
              @@ plainto_tsquery('simple', :q)
            OR body ILIKE concat('%', :q, '%')
          )
        ORDER BY id DESC
        LIMIT 20
        """, nativeQuery = true)
    List<ContentChunk> search(@Param("q") String query, @Param("lang") String language);
}
