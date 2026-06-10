package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.dto.admin.attendance.*;
import com.forgeStackk.EduResolve.security.AdminAuthHelper;
import com.forgeStackk.EduResolve.service.admin.AttendanceAdminService;
import com.forgeStackk.EduResolve.service.admin.AtRiskScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/attendance")
@RequiredArgsConstructor
public class AdminAttendanceController {

    private final AttendanceAdminService service;
    private final AtRiskScanService      scanService;
    private final AdminAuthHelper        adminAuthHelper;

    // ── A: Class summary ──────────────────────────────────────────────────────

    @GetMapping("/summary")
    public List<StudentAttendanceSummaryDto> classSummary(
            @RequestParam Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Principal principal) {

        String schoolName = adminAuthHelper.resolveSchoolName(principal);
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        LocalDate resolvedFrom = from != null ? from : resolvedTo.minusDays(30);
        return service.getClassSummary(classId, resolvedFrom, resolvedTo, schoolName);
    }

    // ── B: Student detail ─────────────────────────────────────────────────────

    @GetMapping("/student/{studentId}")
    public StudentDetailDto studentDetail(
            @PathVariable Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Principal principal) {

        String schoolName = adminAuthHelper.resolveSchoolName(principal);
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        LocalDate resolvedFrom = from != null ? from : resolvedTo.minusDays(30);
        return service.getStudentDetail(studentId, resolvedFrom, resolvedTo, schoolName);
    }

    // ── C: Heatmap ────────────────────────────────────────────────────────────

    @GetMapping("/heatmap")
    public List<HeatmapDayDto> heatmap(
            @RequestParam Long classId,
            @RequestParam(defaultValue = "0") int year,
            Principal principal) {

        String schoolName = adminAuthHelper.resolveSchoolName(principal);
        int resolvedYear = year > 0 ? year : LocalDate.now().getYear();
        return service.getHeatmap(classId, resolvedYear, schoolName);
    }

    // ── D: Insights ───────────────────────────────────────────────────────────

    @GetMapping("/insights")
    public InsightsDto insights(
            @RequestParam Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Principal principal) {

        String schoolName = adminAuthHelper.resolveSchoolName(principal);
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        LocalDate resolvedFrom = from != null ? from : resolvedTo.minusDays(90);
        return service.getInsights(classId, resolvedFrom, resolvedTo, schoolName);
    }

    // ── E: At-risk students ───────────────────────────────────────────────────

    @GetMapping("/at-risk")
    public List<AtRiskStudentDto> atRisk(
            @RequestParam(required = false) Long classId,
            Principal principal) {

        String schoolName = adminAuthHelper.resolveSchoolName(principal);
        return service.getAtRisk(schoolName, classId);
    }

    // ── F: Export ─────────────────────────────────────────────────────────────

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Principal principal) throws Exception {

        String schoolName = adminAuthHelper.resolveSchoolName(principal);
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        LocalDate resolvedFrom = from != null ? from : resolvedTo.minusDays(30);
        byte[] data = service.exportClassCsv(classId, resolvedFrom, resolvedTo, schoolName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"attendance.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(data);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Principal principal) throws Exception {

        String schoolName = adminAuthHelper.resolveSchoolName(principal);
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        LocalDate resolvedFrom = from != null ? from : resolvedTo.minusDays(30);
        String generatedBy = adminAuthHelper.resolveAdminName(principal);
        byte[] data = service.exportClassPdf(classId, resolvedFrom, resolvedTo, schoolName, generatedBy);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"attendance.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    // ── Policy ────────────────────────────────────────────────────────────────

    @GetMapping("/policy")
    public AttendancePolicyDto getPolicy(Principal principal) {
        return service.getPolicy(adminAuthHelper.resolveSchoolName(principal));
    }

    @PutMapping("/policy")
    public AttendancePolicyDto updatePolicy(
            @RequestBody AttendancePolicyDto req,
            Principal principal) {

        String schoolName = adminAuthHelper.resolveSchoolName(principal);
        String updatedBy = adminAuthHelper.resolveAdminName(principal);
        return service.updatePolicy(schoolName, req, updatedBy);
    }

    // ── Anomalies ─────────────────────────────────────────────────────────────

    @GetMapping("/anomalies")
    public List<Map<String, Object>> anomalies(Principal principal) {
        return service.getAnomalies(adminAuthHelper.resolveSchoolName(principal));
    }

    @PostMapping("/anomalies/{id}/acknowledge")
    public ResponseEntity<Void> acknowledgeAnomaly(
            @PathVariable Long id,
            Principal principal) {

        Long userId = adminAuthHelper.resolveUserId(principal);
        service.acknowledgeAnomaly(id, userId);
        return ResponseEntity.noContent().build();
    }

    // ── Manual scan trigger ───────────────────────────────────────────────────

    @PostMapping("/run-scan")
    public ResponseEntity<Map<String, Object>> runScan(Principal principal) {
        String schoolName = adminAuthHelper.resolveSchoolName(principal);
        AtRiskScanService.ScanResult result = scanService.scanSchool(schoolName);
        return ResponseEntity.ok(Map.of(
                "scanned",  result.scanned(),
                "atRisk",   result.atRisk(),
                "notified", result.notified()
        ));
    }
}
