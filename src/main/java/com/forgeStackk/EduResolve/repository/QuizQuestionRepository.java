package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findBySubject(String subject);
    List<QuizQuestion> findBySubjectAndChapter(String subject, String chapter);

    /**
     * Quiz-engine pull: filter by chapterId + difficulty + language, return a
     * randomized slice. Postgres-only (`RANDOM()`).
     */
    @Query(value = """
        SELECT * FROM quiz_question
        WHERE (:chapterId IS NULL OR chapter_id = :chapterId)
          AND (:difficulty IS NULL OR difficulty = :difficulty)
          AND (:language   IS NULL OR language   = :language)
        ORDER BY RANDOM()
        LIMIT :lim
        """, nativeQuery = true)
    List<QuizQuestion> generate(@Param("chapterId") Long chapterId,
                                @Param("difficulty") String difficulty,
                                @Param("language") String language,
                                @Param("lim") Integer lim);
}
