package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.StudentPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentPerformanceRepository extends JpaRepository<StudentPerformance, Long> {

    List<StudentPerformance> findByStudentIdOrderByAccuracyAsc(Long studentId);

    Optional<StudentPerformance> findByStudentIdAndTopicId(Long studentId, Long topicId);

    @Query(nativeQuery = true, value =
        "SELECT sub.name AS subject_name, sub.color_hex, " +
        "       ROUND(AVG(sp.accuracy)) AS avg_score, " +
        "       COUNT(sp.id) AS topic_count " +
        "FROM student_performance sp " +
        "INNER JOIN topic t ON t.id = sp.topic_id " +
        "INNER JOIN chapter c ON c.id = t.chapter_id " +
        "INNER JOIN subject sub ON sub.id = c.subject_id " +
        "WHERE sp.student_id = :studentId " +
        "GROUP BY sub.id, sub.name, sub.color_hex " +
        "ORDER BY avg_score DESC")
    List<Object[]> getSubjectPerformance(@Param("studentId") Long studentId);
}
