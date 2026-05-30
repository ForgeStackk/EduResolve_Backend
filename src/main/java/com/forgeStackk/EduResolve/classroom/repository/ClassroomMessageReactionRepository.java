package com.forgeStackk.EduResolve.classroom.repository;

import com.forgeStackk.EduResolve.classroom.entity.ClassroomMessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface ClassroomMessageReactionRepository extends JpaRepository<ClassroomMessageReaction, Long> {

    List<ClassroomMessageReaction> findByMessageId(Long messageId);

    Optional<ClassroomMessageReaction> findByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, String emoji);

    List<ClassroomMessageReaction> findByUserIdAndMessageIdIn(Long userId, List<Long> messageIds);

    @Query("SELECT r.emoji, COUNT(r) FROM ClassroomMessageReaction r WHERE r.messageId = :messageId GROUP BY r.emoji")
    List<Object[]> countByMessageIdGroupedByEmoji(@Param("messageId") Long messageId);
}
