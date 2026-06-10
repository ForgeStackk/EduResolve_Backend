package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.QuestionBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionBankRepository extends JpaRepository<QuestionBank, Long> {

    List<QuestionBank> findByChapterId(Long chapterId);

    List<QuestionBank> findByChapterIdAndDifficulty(Long chapterId, QuestionBank.Difficulty difficulty);

    List<QuestionBank> findByChapterIdAndDifficultyAndTopic(Long chapterId, QuestionBank.Difficulty difficulty, String topic);

    @Query("SELECT q FROM QuestionBank q WHERE q.chapterId <= :maxChapterId AND q.difficulty = :difficulty ORDER BY FUNCTION('RANDOM')")
    List<QuestionBank> findRandomQuestionsByChapterRangeAndDifficulty(@Param("maxChapterId") Long maxChapterId, @Param("difficulty") QuestionBank.Difficulty difficulty);

    @Query("SELECT q FROM QuestionBank q WHERE q.chapterId <= :maxChapterId ORDER BY FUNCTION('RANDOM')")
    List<QuestionBank> findRandomQuestionsByChapterRange(@Param("maxChapterId") Long maxChapterId);

    List<String> findDistinctTopicByChapterId(Long chapterId);
}
