package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.PreviousYearQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreviousYearQuestionRepository extends JpaRepository<PreviousYearQuestion, Long> {

    List<PreviousYearQuestion> findByChapterIdOrderByYearDesc(Long chapterId);

    List<PreviousYearQuestion> findByChapterIdAndDifficultyOrderByYearDesc(
        Long chapterId, PreviousYearQuestion.Difficulty difficulty);

    List<PreviousYearQuestion> findByChapterIdAndYearOrderByIdAsc(Long chapterId, Integer year);
}
