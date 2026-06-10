package com.forgeStackk.EduResolve.controller.teacher;

import com.forgeStackk.EduResolve.dto.teacher.*;
import com.forgeStackk.EduResolve.security.TeacherPortalAuthHelper;
import com.forgeStackk.EduResolve.service.teacher.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher-portal/attendance")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class TeacherAttendanceController {

    private final AttendanceService attendanceService;
    private final TeacherPortalAuthHelper authHelper;

    // POST /attendance/mark
    @PostMapping("/mark")
    public ResponseEntity<?> mark(@RequestBody MarkAttendanceRequest req) {
        UUID teacherId = authHelper.resolveTeacherId();
        try {
            return ResponseEntity.ok(attendanceService.mark(teacherId, req));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(409).body("Attendance record was modified concurrently. Please retry.");
        }
    }

    // GET /attendance/{classId}?date=YYYY-MM-DD&classLabel=9A
    @GetMapping("/{classId}")
    public ResponseEntity<List<AttendanceRecordResponse>> getByDate(
            @PathVariable UUID classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String classLabel) {
        return ResponseEntity.ok(attendanceService.getByClassAndDate(classId, classLabel, date));
    }

    // PUT /attendance/update
    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody MarkAttendanceRequest req) {
        UUID teacherId = authHelper.resolveTeacherId();
        try {
            return ResponseEntity.ok(attendanceService.update(teacherId, req));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(409).body("Attendance record was modified concurrently. Please retry.");
        }
    }

    // POST /attendance/report/generate
    @PostMapping("/report/generate")
    public ResponseEntity<?> generateReport(@RequestBody GenerateReportRequest req) {
        UUID teacherId = authHelper.resolveTeacherId();
        try {
            return ResponseEntity.ok(attendanceService.generateReport(teacherId, req));
        } catch (Exception e) {
            log.error("Report generation failed", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    // POST /attendance/report/{reportId}/send
    @PostMapping("/report/{reportId}/send")
    public ResponseEntity<?> sendReport(
            @PathVariable UUID reportId,
            @RequestBody SendReportRequest req) {
        UUID teacherId = authHelper.resolveTeacherId();
        try {
            attendanceService.sendReport(teacherId, reportId, req);
            return ResponseEntity.ok().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Report send failed for reportId={}", reportId, e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    // GET /attendance/report/{classId}?month=MM&year=YYYY
    @GetMapping("/report/{classId}")
    public ResponseEntity<?> getReport(
            @PathVariable UUID classId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        try {
            return ResponseEntity.ok(attendanceService.getReport(classId, month, year));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
