package com.forgeStackk.EduResolve.classroom.repository;

import com.forgeStackk.EduResolve.classroom.entity.ClassroomMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassroomMessageRepository extends JpaRepository<ClassroomMessage, Long> {

    @Query("""
        SELECT m FROM ClassroomMessage m
        WHERE m.roomId = :roomId
          AND (:beforeId IS NULL OR m.id < :beforeId)
        ORDER BY m.createdAt DESC
        """)
    List<ClassroomMessage> findByRoomCursor(
        @Param("roomId") Long roomId,
        @Param("beforeId") Long beforeId,
        Pageable pageable);

    Optional<ClassroomMessage> findByIdAndRoomId(Long id, Long roomId);

    @Query("""
        SELECT m FROM ClassroomMessage m
        WHERE m.roomId = :roomId
          AND m.isDeleted = false
          AND LOWER(m.textContent) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY m.createdAt DESC
        """)
    Page<ClassroomMessage> searchInRoom(@Param("roomId") Long roomId, @Param("q") String q, Pageable pageable);
}
