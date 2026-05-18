package com.forgeStackk.EduResolve.repository.teacher;

import com.forgeStackk.EduResolve.entity.teacher.Teacher;
import com.forgeStackk.EduResolve.enums.TeacherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, UUID> {
    Optional<Teacher> findByEmail(String email);
    Optional<Teacher> findByUserId(Long userId);
    List<Teacher> findByStatus(TeacherStatus status);
    Optional<Teacher> findByClassTeacherOf(UUID classId);
}
