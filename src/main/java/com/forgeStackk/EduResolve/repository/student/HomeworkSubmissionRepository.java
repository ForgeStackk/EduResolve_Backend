package com.forgeStackk.EduResolve.repository.student;

import com.forgeStackk.EduResolve.entity.student.HomeworkSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HomeworkSubmissionRepository extends JpaRepository<HomeworkSubmission, Long> {
    List<HomeworkSubmission> findByAssignmentIdOrderBySubmittedAtDesc(Long assignmentId);
    Optional<HomeworkSubmission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
    List<HomeworkSubmission> findByStudentIdOrderBySubmittedAtDesc(Long studentId);
}
