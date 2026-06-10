package com.forgeStackk.EduResolve.repository.teacher;

import com.forgeStackk.EduResolve.entity.teacher.TeacherSubjectMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeacherSubjectMappingRepository extends JpaRepository<TeacherSubjectMapping, UUID> {
    List<TeacherSubjectMapping> findByTeacherId(UUID teacherId);
    List<TeacherSubjectMapping> findByClassId(UUID classId);
    List<TeacherSubjectMapping> findByTeacherIdAndClassId(UUID teacherId, UUID classId);
    List<TeacherSubjectMapping> findByClassIdAndSubjectId(UUID classId, Long subjectId);
    void deleteByTeacherIdAndClassIdAndSubjectId(UUID teacherId, UUID classId, Long subjectId);
}
