package com.forgeStackk.EduResolve.service.teacher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgeStackk.EduResolve.dto.teacher.*;
import com.forgeStackk.EduResolve.entity.teacher.*;
import com.forgeStackk.EduResolve.enums.*;
import com.forgeStackk.EduResolve.entity.teacher.ClassRoom;
import com.forgeStackk.EduResolve.repository.teacher.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final TeacherRepository teacherRepo;
    private final StudentRepository studentRepo;
    private final ClassRoomRepository classRoomRepo;
    private final AttendanceRepository attendanceRepo;
    private final AttendanceReportRepository reportRepo;
    private final MessageRepository messageRepo;
    private final ObjectMapper objectMapper;

    // ── Mark attendance ──────────────────────────────────────────────────────

    @Transactional
    public MarkAttendanceResponse mark(UUID teacherId, MarkAttendanceRequest req) {
        assertClassTeacher(teacherId, req.getClassId());
        String classLabel = req.getClassLabel() != null && !req.getClassLabel().isBlank()
                ? req.getClassLabel().toUpperCase()
                : resolveClassLabel(req.getClassId());

        List<Attendance> toSave = req.getRecords().stream().map(r -> {
            Attendance a = attendanceRepo
                    .findByClassIdAndStudentIdAndDate(classLabel, r.getStudentId(), req.getDate())
                    .orElse(new Attendance());
            a.setClassId(classLabel);
            a.setStudentId(r.getStudentId());
            a.setDate(req.getDate());
            a.setStatus(r.getStatus());
            a.setRemarks(r.getRemarks());
            a.setMarkedBy(teacherId);
            return a;
        }).toList();

        attendanceRepo.saveAll(toSave);
        return new MarkAttendanceResponse(true, toSave.size());
    }

    // ── Get attendance for a class on a date ─────────────────────────────────

    public List<AttendanceRecordResponse> getByClassAndDate(UUID classId, String classLabel, LocalDate date) {
        List<Student> students = studentRepo.findByClassIdAndStatus(classId, StudentStatus.ACTIVE);
        String label = (classLabel != null && !classLabel.isBlank())
                ? classLabel.toUpperCase()
                : resolveClassLabel(classId);
        Map<UUID, Attendance> marked = attendanceRepo.findByClassIdAndDate(label, date)
                .stream().collect(Collectors.toMap(Attendance::getStudentId, a -> a));

        return students.stream().map(s -> new AttendanceRecordResponse(
                s.getStudentId(),
                s.getFullName(),
                s.getRollNumber(),
                marked.containsKey(s.getStudentId()) ? marked.get(s.getStudentId()).getStatus() : null,
                marked.containsKey(s.getStudentId()) ? marked.get(s.getStudentId()).getRemarks() : null
        )).toList();
    }

    // ── Update attendance (same-day correction) ───────────────────────────────

    @Transactional
    public MarkAttendanceResponse update(UUID teacherId, MarkAttendanceRequest req) {
        assertClassTeacher(teacherId, req.getClassId());
        if (!req.getDate().equals(LocalDate.now())) {
            throw new IllegalStateException("Attendance can only be corrected on the same day");
        }
        return mark(teacherId, req);
    }

    // ── Generate monthly report ───────────────────────────────────────────────

    @Transactional
    public GenerateReportResponse generateReport(UUID teacherId, GenerateReportRequest req) {
        YearMonth ym = YearMonth.of(req.getYear(), req.getMonth());
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        String classLabel = resolveClassLabel(req.getClassId());
        List<Student> students = studentRepo.findByClassIdAndStatus(req.getClassId(), StudentStatus.ACTIVE);
        List<Attendance> records = attendanceRepo.findByClassIdAndDateBetween(classLabel, from, to);

        Map<UUID, List<Attendance>> byStudent = records.stream()
                .collect(Collectors.groupingBy(Attendance::getStudentId));

        Set<LocalDate> workingDays = records.stream()
                .filter(a -> a.getStatus() != AttendanceStatus.HOLIDAY)
                .map(Attendance::getDate)
                .collect(Collectors.toSet());

        List<StudentAttendanceSummary> summaries = students.stream().map(s -> {
            List<Attendance> sRecords = byStudent.getOrDefault(s.getStudentId(), List.of());
            int present = count(sRecords, AttendanceStatus.PRESENT);
            int absent = count(sRecords, AttendanceStatus.ABSENT);
            int late = count(sRecords, AttendanceStatus.LATE);
            int halfDay = count(sRecords, AttendanceStatus.HALF_DAY);
            int holiday = count(sRecords, AttendanceStatus.HOLIDAY);
            return new StudentAttendanceSummary(s.getStudentId(), s.getFullName(), present, absent, late, halfDay, holiday);
        }).toList();

        AttendanceSummaryDto summaryDto = new AttendanceSummaryDto(workingDays.size(), summaries);

        AttendanceReport report = reportRepo
                .findByClassIdAndMonthAndYear(classLabel, req.getMonth(), req.getYear())
                .orElse(new AttendanceReport());
        report.setClassId(classLabel);
        report.setMonth(req.getMonth());
        report.setYear(req.getYear());
        report.setGeneratedBy(ReportGeneratedBy.TEACHER);
        report.setStatus(ReportStatus.GENERATED);
        report.setSummary(toJson(summaryDto));
        AttendanceReport saved = reportRepo.save(report);

        return new GenerateReportResponse(saved.getReportId(), saved.getReportFileUrl(), summaryDto);
    }

    // ── Send report via internal messages ────────────────────────────────────

    @Transactional
    public void sendReport(UUID teacherId, UUID reportId, SendReportRequest req) {
        AttendanceReport report = reportRepo.findById(reportId)
                .orElseThrow(() -> new NoSuchElementException("Report not found: " + reportId));

        boolean toStudents = req.getSendTo().contains("STUDENT_PORTAL");
        boolean toParents = req.getSendTo().contains("PARENT_PORTAL");

        UUID classUUID = resolveClassUUID(report.getClassId());
        List<Student> students = studentRepo.findByClassIdAndStatus(classUUID, StudentStatus.ACTIVE);

        for (Student student : students) {
            String body = buildReportText(report, student.getStudentId(), report.getSummary());

            if (toStudents) {
                createReportMessage(teacherId, classUUID, student.getStudentId(), body, RecipientType.INDIVIDUAL_STUDENT);
            }
            if (toParents && student.getParentId() != null) {
                createReportMessage(teacherId, classUUID, student.getParentId(), body, RecipientType.INDIVIDUAL_PARENT);
            }
        }

        report.setStatus(resolveStatus(toStudents, toParents));
        reportRepo.save(report);
    }

    // ── Scheduled: auto-generate on last day of month ─────────────────────────
    // Called by AttendanceReportScheduler; public so it's proxyable.

    @Transactional
    public void autoGenerateForAllClasses(int month, int year) {
        log.info("Auto-generating attendance reports for {}/{}", month, year);
        reportRepo.findAll().stream()
                .map(AttendanceReport::getClassId)
                .distinct()
                .forEach(classLabel -> {
                    try {
                        UUID classUUID = resolveClassUUID(classLabel);
                        GenerateReportRequest req = new GenerateReportRequest();
                        req.setClassId(classUUID);
                        req.setMonth(month);
                        req.setYear(year);
                        generateReport(null, req);
                    } catch (Exception e) {
                        log.error("Auto-report failed for class {}: {}", classLabel, e.getMessage());
                    }
                });
    }

    // ── Fetch a saved report ─────────────────────────────────────────────────

    public GenerateReportResponse getReport(UUID classId, Integer month, Integer year) {
        String classLabel = resolveClassLabel(classId);
        AttendanceReport report = reportRepo.findByClassIdAndMonthAndYear(classLabel, month, year)
                .orElseThrow(() -> new NoSuchElementException(
                        "No report found for class=" + classLabel + " month=" + month + " year=" + year));
        AttendanceSummaryDto summary = fromJson(report.getSummary());
        return new GenerateReportResponse(report.getReportId(), report.getReportFileUrl(), summary);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void assertClassTeacher(UUID teacherId, UUID classId) {
        if (teacherId == null) return; // system/cron call — skip check
        Teacher teacher = teacherRepo.findById(teacherId)
                .orElseThrow(() -> new NoSuchElementException("Teacher not found: " + teacherId));
        if (!classId.equals(teacher.getClassTeacherOf())) {
            throw new IllegalStateException("Teacher is not the class teacher of class " + classId);
        }
    }

    private int count(List<Attendance> records, AttendanceStatus status) {
        return (int) records.stream().filter(a -> a.getStatus() == status).count();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private AttendanceSummaryDto fromJson(String json) {
        if (json == null || json.isBlank()) return new AttendanceSummaryDto(0, List.of());
        try {
            return objectMapper.readValue(json, AttendanceSummaryDto.class);
        } catch (Exception e) {
            return new AttendanceSummaryDto(0, List.of());
        }
    }

    private void createReportMessage(UUID senderId, UUID classId, UUID recipientId, String body, RecipientType type) {
        Message msg = new Message();
        msg.setSenderId(senderId);
        msg.setTargetClassId(classId);
        msg.setRecipientType(type);
        msg.setContentType(MessageContentType.TEXT);
        msg.setTextBody(body);
        msg.setIsHomework(false);
        messageRepo.save(msg);
    }

    private String buildReportText(AttendanceReport report, UUID studentId, String summaryJson) {
        return String.format("Attendance Report — %d/%d | Student ID: %s%n%s",
                report.getMonth(), report.getYear(), studentId, summaryJson);
    }

    private ReportStatus resolveStatus(boolean toStudents, boolean toParents) {
        if (toStudents && toParents) return ReportStatus.SENT_TO_STUDENTS;
        if (toParents) return ReportStatus.SENT_TO_PARENTS;
        return ReportStatus.SENT_TO_STUDENTS;
    }

    /** UUID → "9A" */
    private String resolveClassLabel(UUID classId) {
        return classRoomRepo.findById(classId)
                .map(cr -> cr.getClassName().replace("Class ", "") + cr.getSection())
                .orElseThrow(() -> new NoSuchElementException("Classroom not found for id: " + classId));
    }

    /** "9A" → UUID (reverse lookup) */
    private UUID resolveClassUUID(String label) {
        String grade = label.replaceAll("[^0-9]", "");
        String section = label.replaceAll("[0-9]", "").toUpperCase();
        return classRoomRepo.findByClassNameAndSection("Class " + grade, section)
                .map(ClassRoom::getClassId)
                .orElseThrow(() -> new NoSuchElementException("Classroom not found: " + label));
    }
}
