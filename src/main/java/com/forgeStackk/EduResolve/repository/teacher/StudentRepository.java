package com.forgeStackk.EduResolve.repository.teacher;

import com.forgeStackk.EduResolve.entity.teacher.Student;
import com.forgeStackk.EduResolve.enums.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {
    List<Student> findByClassId(UUID classId);
    List<Student> findByClassIdAndStatus(UUID classId, StudentStatus status);
    Optional<Student> findByRollNumberAndClassId(String rollNumber, UUID classId);
    Optional<Student> findByUserId(Long userId);
    Optional<Student> findBySeqId(Long seqId);
}
