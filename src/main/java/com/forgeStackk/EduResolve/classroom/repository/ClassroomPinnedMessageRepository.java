package com.forgeStackk.EduResolve.classroom.repository;

import com.forgeStackk.EduResolve.classroom.entity.ClassroomPinnedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassroomPinnedMessageRepository extends JpaRepository<ClassroomPinnedMessage, Long> {

    List<ClassroomPinnedMessage> findByRoomIdOrderByPinnedAtDesc(Long roomId);

    long countByRoomId(Long roomId);

    void deleteByRoomIdAndMessageId(Long roomId, Long messageId);
}
