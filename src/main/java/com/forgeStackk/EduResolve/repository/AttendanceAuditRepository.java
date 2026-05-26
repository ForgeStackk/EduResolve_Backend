package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.AttendanceAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttendanceAuditRepository extends JpaRepository<AttendanceAudit, Long> {
    List<AttendanceAudit> findByAttendanceIdOrderByChangedAtDesc(UUID attendanceId);
}
