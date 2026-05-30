package com.forgeStackk.EduResolve.classroom.repository;

import com.forgeStackk.EduResolve.classroom.entity.ClassroomReportedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassroomReportedMessageRepository extends JpaRepository<ClassroomReportedMessage, Long> {

    boolean existsByMessageIdAndReportedByUserId(Long messageId, Long reportedByUserId);
}
