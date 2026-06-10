package com.forgeStackk.EduResolve.repository.teacher;

import com.forgeStackk.EduResolve.entity.teacher.AttendanceReport;
import com.forgeStackk.EduResolve.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceReportRepository extends JpaRepository<AttendanceReport, UUID> {
    List<AttendanceReport> findByClassIdOrderByYearDescMonthDesc(String classId);
    Optional<AttendanceReport> findByClassIdAndMonthAndYear(String classId, Integer month, Integer year);
    List<AttendanceReport> findByStatus(ReportStatus status);
}
