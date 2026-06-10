package com.forgeStackk.EduResolve.notes.repository;

import com.forgeStackk.EduResolve.notes.entity.PdfExtractionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PdfExtractionJobRepository extends JpaRepository<PdfExtractionJob, Long> {

    Optional<PdfExtractionJob> findByIdAndStudentId(Long id, Long studentId);
}
