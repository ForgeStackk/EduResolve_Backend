package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.AttendanceAnomaly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceAnomalyRepository extends JpaRepository<AttendanceAnomaly, Long> {
    List<AttendanceAnomaly> findByAcknowledgedAtIsNullOrderByCreatedAtDesc();
    List<AttendanceAnomaly> findByClassIdAndDetectedDate(String classId, LocalDate date);
    Optional<AttendanceAnomaly> findByClassIdAndDetectedDateAndAcknowledgedAtIsNull(String classId, LocalDate date);
}
