package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.StudentPerformance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentPerformanceRepository extends JpaRepository<StudentPerformance, Long> {

    List<StudentPerformance> findByStudentIdOrderByAccuracyAsc(Long studentId);

    Optional<StudentPerformance> findByStudentIdAndTopicId(Long studentId, Long topicId);
}
