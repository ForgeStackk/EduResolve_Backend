package com.forgeStackk.EduResolve.repository.teacher;

import com.forgeStackk.EduResolve.entity.teacher.Teacher;
import com.forgeStackk.EduResolve.enums.TeacherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, UUID> {
    Optional<Teacher> findByEmail(String email);
    Optional<Teacher> findByUserId(Long userId);
    List<Teacher>     findByStatus(TeacherStatus status);
    Optional<Teacher> findByClassTeacherOf(UUID classId);

    /** Returns all ACTIVE teachers whose linked UserLogin has the given schoolName. */
    @Query(value = "SELECT t.* FROM teacher t " +
                   "JOIN user_login ul ON t.user_id = ul.id " +
                   "WHERE ul.school_name = :schoolName AND t.status = 'ACTIVE' " +
                   "ORDER BY t.full_name",
           nativeQuery = true)
    List<Teacher> findActiveBySchool(@Param("schoolName") String schoolName);
}
