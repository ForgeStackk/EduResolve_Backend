package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    Optional<StudentProfile> findByUserId(Long userId);
    List<StudentProfile> findByClassName(String className);
    @org.springframework.data.jpa.repository.Query(
        "SELECT s FROM StudentProfile s WHERE s.grade = :grade OR s.className LIKE CONCAT(:grade, '%') ORDER BY COALESCE(s.streakDays, 0) DESC LIMIT 10"
    )
    List<StudentProfile> findTop10ForGrade(@org.springframework.data.repository.query.Param("grade") String grade);
}
