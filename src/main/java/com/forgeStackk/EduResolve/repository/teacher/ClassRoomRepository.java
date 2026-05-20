package com.forgeStackk.EduResolve.repository.teacher;

import com.forgeStackk.EduResolve.entity.teacher.ClassRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassRoomRepository extends JpaRepository<ClassRoom, UUID> {
    List<ClassRoom> findByAcademicYear(String academicYear);
    List<ClassRoom> findByClassName(String className);
    Optional<ClassRoom> findByClassNameAndSection(String className, String section);
    List<ClassRoom> findByClassTeacherId(UUID teacherId);
    List<ClassRoom> findByClassTeacherIdIsNotNull();
}
