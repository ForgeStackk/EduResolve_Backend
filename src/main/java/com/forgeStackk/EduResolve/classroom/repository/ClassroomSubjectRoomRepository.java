package com.forgeStackk.EduResolve.classroom.repository;

import com.forgeStackk.EduResolve.classroom.entity.ClassroomSubjectRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassroomSubjectRoomRepository extends JpaRepository<ClassroomSubjectRoom, Long> {

    List<ClassroomSubjectRoom> findByClassroomIdAndIsActiveTrueOrderByNameAsc(Long classroomId);

    Optional<ClassroomSubjectRoom> findByIdAndClassroomId(Long id, Long classroomId);
}
