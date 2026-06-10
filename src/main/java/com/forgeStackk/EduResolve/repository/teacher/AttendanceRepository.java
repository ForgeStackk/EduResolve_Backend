package com.forgeStackk.EduResolve.repository.teacher;

import com.forgeStackk.EduResolve.entity.teacher.Attendance;
import com.forgeStackk.EduResolve.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    List<Attendance> findByClassIdAndDate(String classId, LocalDate date);
    Optional<Attendance> findByClassIdAndStudentIdAndDate(String classId, UUID studentId, LocalDate date);
    List<Attendance> findByStudentIdAndDateBetween(UUID studentId, LocalDate from, LocalDate to);
    List<Attendance> findByClassIdAndDateBetween(String classId, LocalDate from, LocalDate to);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.studentId = :studentId AND a.status = :status AND a.date BETWEEN :from AND :to")
    long countByStudentIdAndStatusAndDateBetween(
        @Param("studentId") UUID studentId,
        @Param("status") AttendanceStatus status,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );
}
