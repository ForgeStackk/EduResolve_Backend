package com.forgeStackk.EduResolve.repository.teacher;

import com.forgeStackk.EduResolve.entity.teacher.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findBySenderIdOrderBySentAtDesc(UUID senderId);
    List<Message> findByTargetClassIdOrderBySentAtDesc(UUID classId);

    Page<Message> findBySenderIdOrderBySentAtDesc(UUID senderId, Pageable pageable);
    Page<Message> findByTargetClassIdOrderBySentAtDesc(UUID classId, Pageable pageable);

    @Query("""
        SELECT m FROM Message m
        WHERE m.senderId = :teacherId
          AND m.isHomework = true
          AND (:classId IS NULL OR m.targetClassId = :classId)
          AND (:subjectId IS NULL OR m.targetSubjectId = :subjectId)
        ORDER BY m.sentAt DESC
        """)
    Page<Message> findHomework(
        @Param("teacherId") UUID teacherId,
        @Param("classId") UUID classId,
        @Param("subjectId") Long subjectId,
        Pageable pageable
    );
}
