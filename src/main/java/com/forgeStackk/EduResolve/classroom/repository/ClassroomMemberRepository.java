package com.forgeStackk.EduResolve.classroom.repository;

import com.forgeStackk.EduResolve.classroom.entity.ClassroomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassroomMemberRepository extends JpaRepository<ClassroomMember, Long> {

    Optional<ClassroomMember> findByClassroomIdAndUserId(Long classroomId, Long userId);

    boolean existsByClassroomIdAndUserId(Long classroomId, Long userId);

    List<ClassroomMember> findByClassroomIdOrderByRoleAscJoinedAtAsc(Long classroomId);

    long countByClassroomId(Long classroomId);

    long countByClassroomIdAndIsOnlineTrue(Long classroomId);

    @Modifying
    @Query("UPDATE ClassroomMember m SET m.isOnline = :online, m.lastSeenAt = :now WHERE m.classroomId = :cid AND m.userId = :uid")
    void updatePresence(@Param("cid") Long classroomId, @Param("uid") Long userId,
                        @Param("online") boolean online, @Param("now") Instant now);
}
