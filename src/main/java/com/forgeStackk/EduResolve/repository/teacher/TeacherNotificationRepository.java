package com.forgeStackk.EduResolve.repository.teacher;

import com.forgeStackk.EduResolve.entity.teacher.TeacherNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TeacherNotificationRepository extends JpaRepository<TeacherNotification, UUID> {
    Page<TeacherNotification> findByTeacherIdOrderByCreatedAtDesc(UUID teacherId, Pageable pageable);
    long countByTeacherIdAndIsRead(UUID teacherId, Boolean isRead);
}
