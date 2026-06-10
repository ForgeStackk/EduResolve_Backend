package com.forgeStackk.EduResolve.notes.repository;

import com.forgeStackk.EduResolve.notes.entity.StudentNotePreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentNotePreferenceRepository extends JpaRepository<StudentNotePreference, Long> {

    Optional<StudentNotePreference> findByStudentId(Long studentId);
}
