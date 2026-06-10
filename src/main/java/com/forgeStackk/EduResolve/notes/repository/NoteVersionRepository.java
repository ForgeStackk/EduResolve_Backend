package com.forgeStackk.EduResolve.notes.repository;

import com.forgeStackk.EduResolve.notes.entity.NoteVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteVersionRepository extends JpaRepository<NoteVersion, Long> {

    List<NoteVersion> findByNoteIdOrderByVersionNumberDesc(Long noteId);

    long countByNoteId(Long noteId);

    @Query("SELECT MAX(v.versionNumber) FROM NoteVersion v WHERE v.noteId = :noteId")
    Optional<Integer> findMaxVersionNumber(@Param("noteId") Long noteId);

    @Query("""
        SELECT v FROM NoteVersion v
        WHERE v.noteId = :noteId
        ORDER BY v.versionNumber ASC
        LIMIT 1
        """)
    Optional<NoteVersion> findOldestByNoteId(@Param("noteId") Long noteId);

    @Modifying
    @Query("DELETE FROM NoteVersion v WHERE v.id = :id")
    void deleteById(@Param("id") Long id);
}
