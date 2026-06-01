package com.forgeStackk.EduResolve.classroom.repository;

import com.forgeStackk.EduResolve.classroom.entity.StudyGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyGroupMemberRepository extends JpaRepository<StudyGroupMember, Long> {

    List<StudyGroupMember> findByGroupId(Long groupId);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    @Modifying
    @Query("DELETE FROM StudyGroupMember m WHERE m.groupId = :groupId AND m.userId = :userId")
    void deleteByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
