package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {
    List<QuizResult> findByStudentIdOrderByCompletedAtDesc(Long studentId);
}
