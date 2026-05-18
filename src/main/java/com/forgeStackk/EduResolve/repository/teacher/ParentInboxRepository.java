package com.forgeStackk.EduResolve.repository.teacher;

import com.forgeStackk.EduResolve.entity.teacher.ParentInbox;
import com.forgeStackk.EduResolve.enums.InboxReadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ParentInboxRepository extends JpaRepository<ParentInbox, UUID> {
    Page<ParentInbox> findByParentIdOrderByReceivedAtDesc(UUID parentId, Pageable pageable);
    long countByParentIdAndReadStatus(UUID parentId, InboxReadStatus status);
}
