package com.forgeStackk.EduResolve.notes.repository;

import com.forgeStackk.EduResolve.notes.entity.StudentNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentNoteRepository extends JpaRepository<StudentNote, Long> {

    Optional<StudentNote> findByIdAndStudentIdAndSchoolName(Long id, Long studentId, String schoolName);

    Page<StudentNote> findByStudentIdAndSchoolNameAndIsActiveTrueAndIsArchivedFalse(
        Long studentId, String schoolName, Pageable pageable);

    @Query("""
        SELECT n FROM StudentNote n
        WHERE n.studentId = :studentId
          AND n.schoolName = :schoolName
          AND n.isActive = true
          AND n.isArchived = false
          AND (:language IS NULL OR n.language = :language)
          AND (:subjectId IS NULL OR n.subjectId = :subjectId)
          AND (:sourceType IS NULL OR n.sourceType = :sourceType)
          AND (:isPinned IS NULL OR n.isPinned = :isPinned)
          AND (:isSharedToClassroom IS NULL OR n.isSharedToClassroom = :isSharedToClassroom)
        """)
    Page<StudentNote> findFilteredNoSearch(
        @Param("studentId") Long studentId,
        @Param("schoolName") String schoolName,
        @Param("language") String language,
        @Param("subjectId") Long subjectId,
        @Param("sourceType") String sourceType,
        @Param("isPinned") Boolean isPinned,
        @Param("isSharedToClassroom") Boolean isSharedToClassroom,
        Pageable pageable);

    @Query(value = """
        SELECT * FROM student_notes sn
        WHERE sn.student_id = :studentId
          AND sn.school_name = :schoolName
          AND sn.is_active = true
          AND sn.is_archived = false
          AND (:language IS NULL OR sn.language = :language)
          AND (:subjectId IS NULL OR sn.subject_id = :subjectId)
          AND (:sourceType IS NULL OR sn.source_type = :sourceType)
          AND (:isPinned IS NULL OR sn.is_pinned = :isPinned)
          AND (:isSharedToClassroom IS NULL OR sn.is_shared_to_classroom = :isSharedToClassroom)
          AND (LOWER(sn.title::text) LIKE LOWER('%' || :search || '%')
               OR LOWER(sn.content::text) LIKE LOWER('%' || :search || '%'))
        ORDER BY sn.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM student_notes sn
        WHERE sn.student_id = :studentId
          AND sn.school_name = :schoolName
          AND sn.is_active = true
          AND sn.is_archived = false
          AND (:language IS NULL OR sn.language = :language)
          AND (:subjectId IS NULL OR sn.subject_id = :subjectId)
          AND (:sourceType IS NULL OR sn.source_type = :sourceType)
          AND (:isPinned IS NULL OR sn.is_pinned = :isPinned)
          AND (:isSharedToClassroom IS NULL OR sn.is_shared_to_classroom = :isSharedToClassroom)
          AND (LOWER(sn.title::text) LIKE LOWER('%' || :search || '%')
               OR LOWER(sn.content::text) LIKE LOWER('%' || :search || '%'))
        """,
        nativeQuery = true)
    Page<StudentNote> findFiltered(
        @Param("studentId") Long studentId,
        @Param("schoolName") String schoolName,
        @Param("language") String language,
        @Param("subjectId") Long subjectId,
        @Param("sourceType") String sourceType,
        @Param("isPinned") Boolean isPinned,
        @Param("isSharedToClassroom") Boolean isSharedToClassroom,
        @Param("search") String search,
        Pageable pageable);

    /** Notes in trash: deleted, not yet archived. */
    List<StudentNote> findByStudentIdAndSchoolNameAndIsActiveFalseAndIsArchivedFalse(
        Long studentId, String schoolName);

    /** Notes eligible for archival: deleted more than retentionDays ago. */
    @Query("""
        SELECT n FROM StudentNote n
        WHERE n.isActive = false
          AND n.isArchived = false
          AND n.deletedAt < :cutoff
        """)
    List<StudentNote> findTrashReadyToArchive(@Param("cutoff") Instant cutoff);

    /** Count notes generated today for rate-limit enforcement. */
    @Query("""
        SELECT COUNT(n) FROM StudentNote n
        WHERE n.studentId = :studentId
          AND n.createdAt >= :startOfDay
          AND n.isArchived = false
        """)
    long countTodayNotes(@Param("studentId") Long studentId, @Param("startOfDay") Instant startOfDay);

    @Query("""
        SELECT COUNT(n) FROM StudentNote n
        WHERE n.studentId = :studentId AND n.isActive = true AND n.isArchived = false
        """)
    long countActiveNotes(@Param("studentId") Long studentId);

    /** Related notes: same subject, overlapping tags, exclude self. */
    @Query("""
        SELECT n FROM StudentNote n
        WHERE n.studentId = :studentId
          AND n.isActive = true
          AND n.isArchived = false
          AND n.id <> :excludeId
          AND n.subjectId = :subjectId
        ORDER BY n.createdAt DESC
        """)
    List<StudentNote> findRelated(
        @Param("studentId") Long studentId,
        @Param("subjectId") Long subjectId,
        @Param("excludeId") Long excludeId,
        Pageable pageable);

    @Modifying
    @Query("UPDATE StudentNote n SET n.isArchived = true WHERE n.id IN :ids")
    void archiveByIds(@Param("ids") List<Long> ids);

    @Query("""
        SELECT COUNT(n) FROM StudentNote n
        WHERE n.studentId = :studentId
          AND n.isActive = true
          AND n.isArchived = false
          AND n.language = :language
        """)
    long countByLanguage(@Param("studentId") Long studentId, @Param("language") String language);

    @Query("""
        SELECT MAX(n.createdAt) FROM StudentNote n
        WHERE n.studentId = :studentId AND n.isArchived = false
        """)
    Optional<Instant> findLastGeneratedAt(@Param("studentId") Long studentId);
}
