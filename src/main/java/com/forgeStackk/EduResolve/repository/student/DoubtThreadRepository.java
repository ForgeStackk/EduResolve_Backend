package com.forgeStackk.EduResolve.repository.student;

import com.forgeStackk.EduResolve.entity.student.DoubtThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoubtThreadRepository extends JpaRepository<DoubtThread, Long> {
    List<DoubtThread>     findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<DoubtThread>     findByTeacherIdOrderByCreatedAtDesc(Long teacherId);
    List<DoubtThread>     findByTeacherIdAndStatusOrderByCreatedAtDesc(Long teacherId, String status);
    /** Backed by composite index (teacher_id, student_class, created_at DESC). */
    List<DoubtThread>     findByTeacherIdAndStudentClassOrderByCreatedAtDesc(Long teacherId, String studentClass);
    /** Used to prevent duplicate threads for the same student × teacher pair. */
    Optional<DoubtThread> findFirstByStudentIdAndTeacherIdOrderByCreatedAtDesc(Long studentId, Long teacherId);
}
