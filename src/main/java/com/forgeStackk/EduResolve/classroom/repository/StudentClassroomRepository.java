package com.forgeStackk.EduResolve.classroom.repository;

import com.forgeStackk.EduResolve.classroom.entity.StudentClassroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentClassroomRepository extends JpaRepository<StudentClassroom, Long> {

    Optional<StudentClassroom> findByClassLabelAndSchoolName(String classLabel, String schoolName);

    Optional<StudentClassroom> findByIdAndSchoolName(Long id, String schoolName);
}
