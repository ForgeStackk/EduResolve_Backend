package com.forgeStackk.EduResolve.repository.student;

import com.forgeStackk.EduResolve.entity.student.DoubtThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoubtThreadRepository extends JpaRepository<DoubtThread, Long> {
    List<DoubtThread> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<DoubtThread> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);
    List<DoubtThread> findByTeacherIdAndStatusOrderByCreatedAtDesc(Long teacherId, String status);
}
