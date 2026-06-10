package com.forgeStackk.EduResolve.repository.teacher;

import com.forgeStackk.EduResolve.entity.teacher.StudentInbox;
import com.forgeStackk.EduResolve.enums.InboxReadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StudentInboxRepository extends JpaRepository<StudentInbox, UUID> {
    Page<StudentInbox> findByStudentIdOrderByReceivedAtDesc(UUID studentId, Pageable pageable);
    long countByStudentIdAndReadStatus(UUID studentId, InboxReadStatus status);
}
