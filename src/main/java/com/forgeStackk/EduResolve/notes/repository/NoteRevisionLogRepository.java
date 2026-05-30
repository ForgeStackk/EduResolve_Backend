package com.forgeStackk.EduResolve.notes.repository;

import com.forgeStackk.EduResolve.notes.entity.NoteRevisionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NoteRevisionLogRepository extends JpaRepository<NoteRevisionLog, Long> {
}
