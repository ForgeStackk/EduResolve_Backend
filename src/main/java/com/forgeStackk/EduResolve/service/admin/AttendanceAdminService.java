package com.forgeStackk.EduResolve.service.admin;

import com.forgeStackk.EduResolve.dto.admin.attendance.*;
import com.forgeStackk.EduResolve.entity.*;
import com.forgeStackk.EduResolve.entity.teacher.*;
import com.forgeStackk.EduResolve.enums.AttendanceStatus;
import com.forgeStackk.EduResolve.enums.ReasonCode;
import com.forgeStackk.EduResolve.repository.*;
import com.forgeStackk.EduResolve.repository.teacher.*;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.time.*;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AttendanceAdminService {

    private final AttendanceRepository attendanceRepo;
    private final AttendanceAuditRepository auditRepo;
    private final AttendancePolicyRepository policyRepo;
    private final AtRiskSnapshotRepository snapshotRepo;
    private final AttendanceAnomalyRepository anomalyRepo;
    private final ClassRoomRepository classRoomRepo;
    private final StudentRepository studentRepo;
    private final LeaveApplicationRepository leaveAppRepo;

    // ── A: Class summary ──────────────────────────────────────────────────────

    public List<StudentAttendanceSummaryDto> getClassSummary(
            Long classId, LocalDate from, LocalDate to, String schoolName) {

        ClassRoom room = resolveRoom(classId, schoolName);
        String classLabel = labelOf(room);
        AttendancePolicy policy = getOrDefaultPolicy(schoolName);

        List<Student> students = studentRepo.findByClassId(room.getClassId());
        List<Attendance> records = attendanceRepo.findByClassIdAndDateBetween(classLabel, from, to);

        long periodDays = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate prevFrom = from.minusDays(periodDays);
        LocalDate prevTo = from.minusDays(1);
        List<Attendance> prevRecords = attendanceRepo.findByClassIdAndDateBetween(classLabel, prevFrom, prevTo);

        Map<UUID, List<Attendance>> byStudent = groupByStudent(records);
        Map<UUID, List<Attendance>> prevByStudent = groupByStudent(prevRecords);
        int totalWorkingDays = distinctWorkingDays(records);

        return students.stream().map(s -> {
            List<Attendance> sRec = byStudent.getOrDefault(s.getStudentId(), List.of());
            List<Attendance> sPrev = prevByStudent.getOrDefault(s.getStudentId(), List.of());

            int present = count(sRec, AttendanceStatus.PRESENT);
            int absent = count(sRec, AttendanceStatus.ABSENT);
            int late = count(sRec, AttendanceStatus.LATE);
            int excused = countExcused(sRec);

            double currPct = pct(present + late, excused, totalWorkingDays);
            double prevPct = pct(
                    count(sPrev, AttendanceStatus.PRESENT) + count(sPrev, AttendanceStatus.LATE),
                    countExcused(sPrev), distinctWorkingDays(sPrev));

            boolean atRisk = currPct < policy.getMinAttendancePct()
                    || (prevPct - currPct) >= policy.getAtRiskDropPct();

            return new StudentAttendanceSummaryDto(
                    s.getSeqId(), s.getFullName(), s.getRollNumber(),
                    totalWorkingDays, present, absent, late, excused,
                    round2(currPct), atRisk, trend(currPct, prevPct), dominantReason(sRec));
        }).toList();
    }

    // ── B: Student detail ─────────────────────────────────────────────────────

    public StudentDetailDto getStudentDetail(Long studentId, LocalDate from, LocalDate to, String schoolName) {
        Student student = studentRepo.findBySeqId(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        ClassRoom room = classRoomRepo.findById(student.getClassId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classroom not found"));
        assertInSchool(room, schoolName);

        String classLabel = labelOf(room);
        List<Attendance> records = attendanceRepo.findByStudentIdAndDateBetween(student.getStudentId(), from, to);
        List<Attendance> classRecs = attendanceRepo.findByClassIdAndDateBetween(classLabel, from, to);
        int totalWorkingDays = distinctWorkingDays(classRecs);

        int present = count(records, AttendanceStatus.PRESENT);
        int absent = count(records, AttendanceStatus.ABSENT);
        int late = count(records, AttendanceStatus.LATE);
        int excused = countExcused(records);
        double percentage = pct(present + late, excused, totalWorkingDays);

        List<StudentDayRecordDto> dayRecords = records.stream()
                .sorted(Comparator.comparing(Attendance::getDate))
                .map(a -> new StudentDayRecordDto(
                        a.getDate(), a.getStatus().name(),
                        a.getReasonCode() != null ? a.getReasonCode().name() : null,
                        a.getRemarks(),
                        a.getMarkedBy() != null ? a.getMarkedBy().toString() : null))
                .toList();

        Map<String, Long> reasonsBreakdown = records.stream()
                .filter(a -> a.getReasonCode() != null)
                .collect(Collectors.groupingBy(a -> a.getReasonCode().name(), Collectors.counting()));

        List<LeaveApplicationSummaryDto> leaves = leaveAppRepo
                .findByClassNameOrderByCreatedAtDesc(classLabel).stream()
                .filter(l -> l.getStudentName().equalsIgnoreCase(student.getFullName()))
                .filter(l -> !l.getToDate().isBefore(from) && !l.getFromDate().isAfter(to))
                .map(l -> new LeaveApplicationSummaryDto(l.getId(), l.getFromDate(), l.getToDate(), l.getReason(), l.getStatus()))
                .toList();

        List<AuditEntryDto> auditHistory = records.stream()
                .flatMap(a -> auditRepo.findByAttendanceIdOrderByChangedAtDesc(a.getAttendanceId()).stream())
                .map(au -> new AuditEntryDto(
                        au.getId(), au.getChangedAt(), au.getChangedByUserId(),
                        au.getOldStatus(), au.getNewStatus(),
                        au.getOldReasonCode(), au.getNewReasonCode(), au.getNote()))
                .sorted(Comparator.comparing(AuditEntryDto::changedAt).reversed())
                .toList();

        return new StudentDetailDto(
                studentId, student.getFullName(), classLabel,
                totalWorkingDays, present, absent, late, excused,
                round2(percentage), dayRecords, reasonsBreakdown, leaves, auditHistory);
    }

    // ── C: Heatmap (cached) ───────────────────────────────────────────────────

    @Cacheable(value = "heatmap", key = "#classId + ':' + #year")
    public List<HeatmapDayDto> getHeatmap(Long classId, int year, String schoolName) {
        ClassRoom room = resolveRoom(classId, schoolName);
        String classLabel = labelOf(room);
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        int totalStudents = studentRepo.findByClassId(room.getClassId()).size();

        return attendanceRepo.findByClassIdAndDateBetween(classLabel, from, to).stream()
                .filter(a -> a.getStatus() != AttendanceStatus.HOLIDAY)
                .collect(Collectors.groupingBy(Attendance::getDate))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    long present = e.getValue().stream()
                            .filter(a -> a.getStatus() == AttendanceStatus.PRESENT
                                    || a.getStatus() == AttendanceStatus.LATE)
                            .count();
                    double p = totalStudents > 0 ? present * 100.0 / totalStudents : 0.0;
                    return new HeatmapDayDto(e.getKey(), (int) present, totalStudents, round2(p));
                })
                .toList();
    }

    // ── D: Insights (cached) ──────────────────────────────────────────────────

    @Cacheable(value = "insights", key = "#classId + ':' + #from + ':' + #to")
    public InsightsDto getInsights(Long classId, LocalDate from, LocalDate to, String schoolName) {
        ClassRoom room = resolveRoom(classId, schoolName);
        String classLabel = labelOf(room);
        int totalStudents = studentRepo.findByClassId(room.getClassId()).size();
        if (totalStudents == 0) return new InsightsDto(Map.of(), Map.of(), List.of(), Map.of());

        List<Attendance> records = attendanceRepo.findByClassIdAndDateBetween(classLabel, from, to);

        Map<String, Double> dowBreakdown = records.stream()
                .filter(a -> a.getStatus() != AttendanceStatus.HOLIDAY)
                .collect(Collectors.groupingBy(
                        a -> a.getDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                        Collectors.collectingAndThen(Collectors.toList(), dayRecs -> {
                            long days = dayRecs.stream().map(Attendance::getDate).distinct().count();
                            long present = dayRecs.stream()
                                    .filter(a -> a.getStatus() == AttendanceStatus.PRESENT
                                            || a.getStatus() == AttendanceStatus.LATE).count();
                            return days > 0 ? round2(present * 100.0 / ((double) days * totalStudents)) : 0.0;
                        })));

        Map<String, Long> reasonsBreakdown = records.stream()
                .filter(a -> a.getReasonCode() != null)
                .collect(Collectors.groupingBy(a -> a.getReasonCode().name(), Collectors.counting()));

        List<InsightsDto.MonthlyTrendPointDto> monthlyTrend = buildMonthlyTrend(classLabel, totalStudents);
        Map<String, Double> classComparison = buildClassComparison(from, to, schoolName);

        return new InsightsDto(dowBreakdown, reasonsBreakdown, monthlyTrend, classComparison);
    }

    // ── E: At-risk ────────────────────────────────────────────────────────────

    public List<AtRiskStudentDto> getAtRisk(String schoolName, Long classId) {
        AttendancePolicy policy = getOrDefaultPolicy(schoolName);
        int threshold = policy.getMinAttendancePct();
        int dropThreshold = policy.getAtRiskDropPct();

        LocalDate today = LocalDate.now();
        LocalDate currFrom = today.minusDays(30);
        LocalDate prevFrom = today.minusDays(60);
        LocalDate prevTo = today.minusDays(31);

        List<ClassRoom> rooms = classId != null
                ? classRoomRepo.findBySeqId(classId)
                        .filter(r -> r.getSchoolName() != null && r.getSchoolName().equalsIgnoreCase(schoolName))
                        .map(List::of).orElse(List.of())
                : classRoomRepo.findBySchoolNameIgnoreCase(schoolName);

        return rooms.stream().flatMap(room -> {
            String classLabel = labelOf(room);
            List<Student> students = studentRepo.findByClassId(room.getClassId());
            if (students.isEmpty()) return Stream.empty();

            List<Long> seqIds = students.stream().map(Student::getSeqId).toList();
            Map<Long, AtRiskSnapshot> snapshots = snapshotRepo.findLatestForStudents(seqIds).stream()
                    .collect(Collectors.toMap(AtRiskSnapshot::getStudentSeqId, s -> s));

            List<Attendance> currRecs = attendanceRepo.findByClassIdAndDateBetween(classLabel, currFrom, today);
            List<Attendance> prevRecs = attendanceRepo.findByClassIdAndDateBetween(classLabel, prevFrom, prevTo);
            int currTotal = distinctWorkingDays(currRecs);
            int prevTotal = distinctWorkingDays(prevRecs);

            Map<UUID, List<Attendance>> currByS = groupByStudent(currRecs);
            Map<UUID, List<Attendance>> prevByS = groupByStudent(prevRecs);

            return students.stream().flatMap(s -> {
                List<Attendance> curr = currByS.getOrDefault(s.getStudentId(), List.of());
                List<Attendance> prev = prevByS.getOrDefault(s.getStudentId(), List.of());

                double currPct = pct(count(curr, AttendanceStatus.PRESENT) + count(curr, AttendanceStatus.LATE),
                        countExcused(curr), currTotal);
                double prevPct = pct(count(prev, AttendanceStatus.PRESENT) + count(prev, AttendanceStatus.LATE),
                        countExcused(prev), prevTotal);
                double drop = prevPct - currPct;

                if (currPct >= threshold && drop < dropThreshold) return Stream.empty();

                String severity = currPct < (threshold - 10) ? "HIGH" : (currPct < threshold ? "MEDIUM" : "LOW");
                LocalDate lastAbsent = curr.stream()
                        .filter(a -> a.getStatus() == AttendanceStatus.ABSENT)
                        .map(Attendance::getDate).max(Comparator.naturalOrder()).orElse(null);
                AtRiskSnapshot snap = snapshots.get(s.getSeqId());
                Instant lastNotifiedAt = snap != null ? snap.getNotifiedAt() : null;
                long daysSinceNotified = lastNotifiedAt != null
                        ? Duration.between(lastNotifiedAt, Instant.now()).toDays() : -1L;

                return Stream.of(new AtRiskStudentDto(
                        s.getSeqId(), s.getFullName(), classLabel, s.getRollNumber(),
                        round2(currPct), round2(prevPct), round2(drop), severity,
                        lastAbsent, dominantReason(curr), lastNotifiedAt, daysSinceNotified));
            });
        }).toList();
    }

    // ── F: Export ─────────────────────────────────────────────────────────────

    public byte[] exportClassCsv(Long classId, LocalDate from, LocalDate to, String schoolName) throws IOException {
        List<StudentAttendanceSummaryDto> rows = getClassSummary(classId, from, to, schoolName);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CSVWriter csv = new CSVWriter(new OutputStreamWriter(out))) {
            csv.writeNext(new String[]{
                    "Student ID", "Name", "Roll No", "Total Days", "Present",
                    "Absent", "Late", "Excused", "Percentage", "At Risk", "Trend", "Dominant Reason"});
            for (var r : rows) {
                csv.writeNext(new String[]{
                        r.studentId().toString(), r.name(), r.rollNumber(),
                        String.valueOf(r.totalDays()), String.valueOf(r.present()),
                        String.valueOf(r.absent()), String.valueOf(r.late()),
                        String.valueOf(r.excused()), String.format("%.1f%%", r.percentage()),
                        r.atRisk() ? "Yes" : "No", r.trend(),
                        r.dominantAbsenceReason() != null ? r.dominantAbsenceReason() : ""
                });
            }
        }
        return out.toByteArray();
    }

    public byte[] exportClassPdf(Long classId, LocalDate from, LocalDate to,
                                  String schoolName, String generatedBy) throws Exception {
        List<StudentAttendanceSummaryDto> rows = getClassSummary(classId, from, to, schoolName);
        AttendancePolicy policy = getOrDefaultPolicy(schoolName);
        ClassRoom room = classRoomRepo.findBySeqId(classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classroom not found"));

        String generatedAt = java.time.format.DateTimeFormatter
                .ofPattern("dd MMM yyyy HH:mm")
                .withZone(java.time.ZoneOffset.UTC)
                .format(Instant.now()) + " UTC";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, out);
        doc.open();
        doc.add(new Paragraph("Attendance Report — Class " + labelOf(room)));
        doc.add(new Paragraph("School: " + schoolName));
        doc.add(new Paragraph("Period: " + from + " to " + to));
        doc.add(new Paragraph("Generated by: " + generatedBy + "   |   Generated at: " + generatedAt));
        doc.add(new Paragraph("Min. attendance: " + policy.getMinAttendancePct()
                + "%   |   At-risk drop threshold: " + policy.getAtRiskDropPct() + "%"));
        doc.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(12);
        table.setWidthPercentage(100);
        for (String h : new String[]{"Student ID", "Name", "Roll No", "Total", "Present", "Absent", "Late", "Excused", "Pct", "At Risk", "Trend", "Reason"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h));
            cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
            table.addCell(cell);
        }
        for (var r : rows) {
            table.addCell(r.studentId().toString());
            table.addCell(r.name());
            table.addCell(r.rollNumber());
            table.addCell(String.valueOf(r.totalDays()));
            table.addCell(String.valueOf(r.present()));
            table.addCell(String.valueOf(r.absent()));
            table.addCell(String.valueOf(r.late()));
            table.addCell(String.valueOf(r.excused()));
            table.addCell(String.format("%.1f%%", r.percentage()));
            table.addCell(r.atRisk() ? "Yes" : "No");
            table.addCell(r.trend());
            table.addCell(r.dominantAbsenceReason() != null ? r.dominantAbsenceReason() : "");
        }
        doc.add(table);
        doc.close();
        return out.toByteArray();
    }

    // ── Policy ────────────────────────────────────────────────────────────────

    public AttendancePolicyDto getPolicy(String schoolName) {
        return toDto(getOrDefaultPolicy(schoolName));
    }

    @Transactional
    @CacheEvict(value = {"heatmap", "insights"}, allEntries = true)
    public AttendancePolicyDto updatePolicy(String schoolName, AttendancePolicyDto req, String updatedBy) {
        AttendancePolicy p = policyRepo.findBySchoolNameIgnoreCase(schoolName).orElseGet(() -> {
            AttendancePolicy n = new AttendancePolicy();
            n.setSchoolName(schoolName);
            return n;
        });
        p.setMinAttendancePct(req.minAttendancePct());
        p.setAtRiskDropPct(req.atRiskDropPct());
        p.setAutoNotifyParents(req.autoNotifyParents());
        p.setAutoNotifyThresholdPct(req.autoNotifyThresholdPct());
        p.setAutoNotifyCooldownDays(req.autoNotifyCooldownDays());
        p.setLastUpdatedBy(updatedBy);
        p.setLastUpdatedAt(Instant.now());
        policyRepo.save(p);
        return toDto(p);
    }

    // ── Anomalies ─────────────────────────────────────────────────────────────

    public List<Map<String, Object>> getAnomalies(String schoolName) {
        Set<String> schoolLabels = classRoomRepo.findBySchoolNameIgnoreCase(schoolName).stream()
                .map(this::labelOf).collect(Collectors.toSet());
        return anomalyRepo.findByAcknowledgedAtIsNullOrderByCreatedAtDesc().stream()
                .filter(a -> schoolLabels.contains(a.getClassId()))
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.getId());
                    m.put("classId", a.getClassId());
                    m.put("detectedDate", a.getDetectedDate());
                    m.put("classAvgPct", a.getClassAvgPct());
                    m.put("expectedPct", a.getExpectedPct());
                    m.put("dropAmount", a.getDropAmount());
                    m.put("createdAt", a.getCreatedAt());
                    return m;
                }).toList();
    }

    @Transactional
    public void acknowledgeAnomaly(Long id, Long userId) {
        AttendanceAnomaly anomaly = anomalyRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anomaly not found: " + id));
        if (anomaly.getAcknowledgedAt() != null) return;
        anomaly.setAcknowledgedByUserId(userId);
        anomaly.setAcknowledgedAt(Instant.now());
        anomalyRepo.save(anomaly);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ClassRoom resolveRoom(Long classId, String schoolName) {
        ClassRoom room = classRoomRepo.findBySeqId(classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classroom not found"));
        assertInSchool(room, schoolName);
        return room;
    }

    private String resolveLabel(Long classId, String schoolName) {
        return labelOf(resolveRoom(classId, schoolName));
    }

    private String labelOf(ClassRoom room) {
        return (room.getClassName().replace("Class ", "") + room.getSection()).toUpperCase();
    }

    private void assertInSchool(ClassRoom room, String schoolName) {
        if (room.getSchoolName() == null || !room.getSchoolName().equalsIgnoreCase(schoolName)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this class");
        }
    }

    private AttendancePolicy getOrDefaultPolicy(String schoolName) {
        return policyRepo.findBySchoolNameIgnoreCase(schoolName).orElseGet(AttendancePolicy::new);
    }

    private AttendancePolicyDto toDto(AttendancePolicy p) {
        return new AttendancePolicyDto(
                p.getMinAttendancePct(), p.getAtRiskDropPct(),
                p.isAutoNotifyParents(), p.getAutoNotifyThresholdPct(),
                p.getAutoNotifyCooldownDays(), p.getLastUpdatedBy(), p.getLastUpdatedAt());
    }

    private Map<UUID, List<Attendance>> groupByStudent(List<Attendance> records) {
        return records.stream().collect(Collectors.groupingBy(Attendance::getStudentId));
    }

    private int distinctWorkingDays(List<Attendance> records) {
        return (int) records.stream()
                .filter(a -> a.getStatus() != AttendanceStatus.HOLIDAY)
                .map(Attendance::getDate).distinct().count();
    }

    private int count(List<Attendance> records, AttendanceStatus status) {
        return (int) records.stream().filter(a -> a.getStatus() == status).count();
    }

    private int countExcused(List<Attendance> records) {
        return (int) records.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT)
                .filter(a -> a.getReasonCode() == ReasonCode.LEAVE || a.getReasonCode() == ReasonCode.MEDICAL)
                .count();
    }

    private double pct(int presentEquiv, int excused, int total) {
        return total > 0 ? (presentEquiv + excused) * 100.0 / total : 0.0;
    }

    private String trend(double current, double previous) {
        double diff = current - previous;
        if (diff > 2.0) return "UP";
        if (diff < -2.0) return "DOWN";
        return "STABLE";
    }

    private String dominantReason(List<Attendance> records) {
        return records.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT && a.getReasonCode() != null)
                .collect(Collectors.groupingBy(a -> a.getReasonCode().name(), Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(null);
    }

    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    private List<InsightsDto.MonthlyTrendPointDto> buildMonthlyTrend(String classLabel, int totalStudents) {
        LocalDate now = LocalDate.now();
        List<InsightsDto.MonthlyTrendPointDto> result = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = YearMonth.from(now).minusMonths(i);
            LocalDate mFrom = ym.atDay(1);
            LocalDate mTo = ym.atEndOfMonth();
            List<Attendance> recs = attendanceRepo.findByClassIdAndDateBetween(classLabel, mFrom, mTo);
            int mTotal = distinctWorkingDays(recs);
            long present = recs.stream()
                    .filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE)
                    .count();
            double p = (mTotal > 0 && totalStudents > 0) ? present * 100.0 / ((long) mTotal * totalStudents) : 0.0;
            result.add(new InsightsDto.MonthlyTrendPointDto(ym.toString(), round2(p)));
        }
        return result;
    }

    private Map<String, Double> buildClassComparison(LocalDate from, LocalDate to, String schoolName) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (ClassRoom room : classRoomRepo.findBySchoolNameIgnoreCase(schoolName)) {
            String label = labelOf(room);
            int roomStudents = studentRepo.findByClassId(room.getClassId()).size();
            if (roomStudents == 0) continue;
            List<Attendance> recs = attendanceRepo.findByClassIdAndDateBetween(label, from, to);
            int total = distinctWorkingDays(recs);
            long present = recs.stream()
                    .filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE)
                    .count();
            result.put(label, total > 0 ? round2(present * 100.0 / ((long) total * roomStudents)) : 0.0);
        }
        return result;
    }
}
