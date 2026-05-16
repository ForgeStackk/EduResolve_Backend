package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.ReadingProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, Long> {
    Optional<ReadingProgress> findFirstByStudentIdOrderByLastReadAtDesc(Long studentId);
}
