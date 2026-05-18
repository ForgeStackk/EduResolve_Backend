package com.forgeStackk.EduResolve.scheduler;

import com.forgeStackk.EduResolve.dto.teacher.GenerateReportRequest;
import com.forgeStackk.EduResolve.entity.teacher.*;
import com.forgeStackk.EduResolve.enums.StudentStatus;
import com.forgeStackk.EduResolve.repository.teacher.*;
import com.forgeStackk.EduResolve.service.teacher.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AttendanceReportScheduler {

    private final AttendanceService    attendanceService;
    private final NotificationService  notificationService;
    private final AttendancePdfService pdfService;
    private final ReportStorageService storageService;

    private final ClassRoomRepository  classRoomRepo;
    private final StudentRepository    studentRepo;
    private final TeacherRepository    teacherRepo;
    private final AttendanceRepository attendanceRepo;

    // ── 6 PM on the last day of every month ──────────────────────────────────

    @Scheduled(cron = "0 0 18 L * ?")
    public void autoGenerateMonthlyReports() {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year  = today.getYear();
        log.info("Monthly attendance report job started for {}/{}", month, year);

        List<ClassRoom> classes = classRoomRepo.findByClassTeacherIdIsNotNull();
        log.info("Processing {} active class(es)", classes.size());

        for (ClassRoom cls : classes) {
            try {
                processClass(cls, month, year);
            } catch (Exception e) {
                log.error("Report job failed for class={}: {}", cls.getClassId(), e.getMessage(), e);
            }
        }

        log.info("Monthly attendance report job completed for {}/{}", month, year);
    }

    // ── Per-class processing ──────────────────────────────────────────────────

    private void processClass(ClassRoom cls, int month, int year) throws Exception {
        UUID classId = cls.getClassId();

        // 1. Generate/refresh the aggregate AttendanceReport entity
        GenerateReportRequest req = new GenerateReportRequest();
        req.setClassId(classId);
        req.setMonth(month);
        req.setYear(year);
        int totalWorkingDays = attendanceService.generateReport(null, req)
                .getSummary()
                .getTotalWorkingDays();

        // 2. Resolve class teacher name for the footer
        String teacherName = cls.getClassTeacherId() != null
                ? teacherRepo.findById(cls.getClassTeacherId())
                        .map(Teacher::getFullName)
                        .orElse("Class Teacher")
                : "Class Teacher";

        String classLabel = cls.getClassName() + "-" + cls.getSection();

        // 3. Generate a PDF per student and store it
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to   = ym.atEndOfMonth();

        List<Student> students = studentRepo.findByClassIdAndStatus(classId, StudentStatus.ACTIVE);
        log.info("Generating PDFs for {} student(s) in class {}", students.size(), classLabel);

        for (Student student : students) {
            List<Attendance> records = attendanceRepo
                    .findByStudentIdAndDateBetween(student.getStudentId(), from, to);

            byte[] pdf = pdfService.generate(
                    cls.getClassName(),
                    cls.getSection(),
                    month,
                    year,
                    student.getFullName(),
                    student.getRollNumber(),
                    teacherName,
                    records,
                    totalWorkingDays);

            storageService.store(classId, year, month, student.getStudentId(), pdf);
        }

        // 4. Notify the class teacher
        if (cls.getClassTeacherId() != null) {
            notificationService.push(
                    cls.getClassTeacherId(),
                    "Monthly attendance report for " + classLabel + " ("
                    + ym.getMonth().name().charAt(0)
                    + ym.getMonth().name().substring(1).toLowerCase()
                    + " " + year + ") is ready.");
        }

        log.info("Finished processing class {} for {}/{}", classLabel, month, year);
    }
}
