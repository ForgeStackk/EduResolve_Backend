package com.forgeStackk.EduResolve.classroom.repository;

import com.forgeStackk.EduResolve.classroom.entity.StudyGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {

    /** Groups where the user is the owner OR a member. */
    @Query("""
        SELECT g FROM StudyGroup g
        WHERE g.classroomId = :classroomId
          AND (g.ownerUserId = :userId
               OR EXISTS (
                   SELECT m FROM StudyGroupMember m
                   WHERE m.groupId = g.id AND m.userId = :userId
               ))
        ORDER BY g.createdAt DESC
        """)
    List<StudyGroup> findMyGroups(@Param("classroomId") Long classroomId,
                                  @Param("userId")      Long userId);
}
