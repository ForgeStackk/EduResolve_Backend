package com.forgeStackk.EduResolve.service.admin;

import com.forgeStackk.EduResolve.dto.admin.attendance.StudentAttendanceSummaryDto;
import com.forgeStackk.EduResolve.entity.AttendanceAnomaly;
import com.forgeStackk.EduResolve.entity.AttendancePolicy;
import com.forgeStackk.EduResolve.entity.teacher.Attendance;
import com.forgeStackk.EduResolve.entity.teacher.ClassRoom;
import com.forgeStackk.EduResolve.entity.teacher.Student;
import com.forgeStackk.EduResolve.enums.AttendanceStatus;
import com.forgeStackk.EduResolve.repository.*;
import com.forgeStackk.EduResolve.repository.teacher.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceAdminServiceTest {

    @Mock AttendanceRepository attendanceRepo;
    @Mock AttendanceAuditRepository auditRepo;
    @Mock AttendancePolicyRepository policyRepo;
    @Mock AtRiskSnapshotRepository snapshotRepo;
    @Mock AttendanceAnomalyRepository anomalyRepo;
    @Mock ClassRoomRepository classRoomRepo;
    @Mock StudentRepository studentRepo;
    @Mock LeaveApplicationRepository leaveAppRepo;

    @InjectMocks AttendanceAdminService service;

    private static final String SCHOOL      = "Test School";
    private static final Long   CLASS_SEQ   = 1L;
    private static final Long   STUDENT_SEQ = 1L;
    private static final UUID   CLASS_UUID  = UUID.randomUUID();
    private static final UUID   STUDENT_UUID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate FROM  = TODAY.minusDays(30);

    private ClassRoom room;
    private Student student;
    private AttendancePolicy policy;

    @BeforeEach
    void setUp() {
        room    = new ClassRoom(CLASS_UUID, "Class 9", "A", "2025-26", null, SCHOOL, CLASS_SEQ);
        student = new Student(STUDENT_UUID, "Ravi Kumar", "001", CLASS_UUID, null, null, null, STUDENT_SEQ);
        policy  = new AttendancePolicy(null, SCHOOL, 75, 10, false, 75, 7, null, null);
    }

    // ── Percentage calculation ────────────────────────────────────────────────

    @Test
    void classSummary_calculatesPercentageCorrectly() {
        when(classRoomRepo.findBySeqId(CLASS_SEQ)).thenReturn(Optional.of(room));
        when(policyRepo.findBySchoolNameIgnoreCase(SCHOOL)).thenReturn(Optional.of(policy));
        when(studentRepo.findByClassId(CLASS_UUID)).thenReturn(List.of(student));

        // 20 working days, student present 15
        List<Attendance> records = buildAttendance(STUDENT_UUID, "9A", FROM, 15, 5);
        when(attendanceRepo.findByClassIdAndDateBetween(eq("9A"), any(), any()))
                .thenReturn(records).thenReturn(List.of()); // prev period empty

        List<StudentAttendanceSummaryDto> result = service.getClassSummary(CLASS_SEQ, FROM, TODAY, SCHOOL);

        assertThat(result).hasSize(1);
        // 15 present out of 20 working days = 75%
        assertThat(result.get(0).percentage()).isEqualTo(75.0);
        assertThat(result.get(0).present()).isEqualTo(15);
        assertThat(result.get(0).absent()).isEqualTo(5);
    }

    // ── atRisk detection ─────────────────────────────────────────────────────

    @Test
    void classSummary_flagsAtRiskWhenBelowThreshold() {
        when(classRoomRepo.findBySeqId(CLASS_SEQ)).thenReturn(Optional.of(room));
        when(policyRepo.findBySchoolNameIgnoreCase(SCHOOL)).thenReturn(Optional.of(policy));
        when(studentRepo.findByClassId(CLASS_UUID)).thenReturn(List.of(student));

        // 20 days, 14 present = 70% (below 75% threshold)
        List<Attendance> records = buildAttendance(STUDENT_UUID, "9A", FROM, 14, 6);
        when(attendanceRepo.findByClassIdAndDateBetween(eq("9A"), any(), any()))
                .thenReturn(records).thenReturn(List.of());

        List<StudentAttendanceSummaryDto> result = service.getClassSummary(CLASS_SEQ, FROM, TODAY, SCHOOL);

        assertThat(result.get(0).atRisk()).isTrue();
    }

    @Test
    void classSummary_flagsAtRiskOnBigDrop() {
        when(classRoomRepo.findBySeqId(CLASS_SEQ)).thenReturn(Optional.of(room));
        when(policyRepo.findBySchoolNameIgnoreCase(SCHOOL)).thenReturn(Optional.of(policy));
        when(studentRepo.findByClassId(CLASS_UUID)).thenReturn(List.of(student));

        // current: 16/20 = 80% (above threshold)
        List<Attendance> current = buildAttendance(STUDENT_UUID, "9A", FROM, 16, 4);
        // prev: 19/20 = 95% → drop = 15% which is >= atRiskDropPct(10)
        List<Attendance> prev = buildAttendance(STUDENT_UUID, "9A", FROM.minusDays(31), 19, 1);

        when(attendanceRepo.findByClassIdAndDateBetween(eq("9A"), any(), any()))
                .thenReturn(current).thenReturn(prev);

        List<StudentAttendanceSummaryDto> result = service.getClassSummary(CLASS_SEQ, FROM, TODAY, SCHOOL);

        assertThat(result.get(0).atRisk()).isTrue();
    }

    @Test
    void classSummary_notAtRiskWhenOk() {
        when(classRoomRepo.findBySeqId(CLASS_SEQ)).thenReturn(Optional.of(room));
        when(policyRepo.findBySchoolNameIgnoreCase(SCHOOL)).thenReturn(Optional.of(policy));
        when(studentRepo.findByClassId(CLASS_UUID)).thenReturn(List.of(student));

        // 16/20 = 80% (above 75%), small drop from prev (17/20 = 85%, drop = 5%)
        List<Attendance> current = buildAttendance(STUDENT_UUID, "9A", FROM, 16, 4);
        List<Attendance> prev = buildAttendance(STUDENT_UUID, "9A", FROM.minusDays(31), 17, 3);

        when(attendanceRepo.findByClassIdAndDateBetween(eq("9A"), any(), any()))
                .thenReturn(current).thenReturn(prev);

        List<StudentAttendanceSummaryDto> result = service.getClassSummary(CLASS_SEQ, FROM, TODAY, SCHOOL);

        assertThat(result.get(0).atRisk()).isFalse();
    }

    // ── Trend ─────────────────────────────────────────────────────────────────

    @Test
    void classSummary_trendIsUp() {
        when(classRoomRepo.findBySeqId(CLASS_SEQ)).thenReturn(Optional.of(room));
        when(policyRepo.findBySchoolNameIgnoreCase(SCHOOL)).thenReturn(Optional.of(policy));
        when(studentRepo.findByClassId(CLASS_UUID)).thenReturn(List.of(student));

        // current: 18/20 = 90%; prev: 15/20 = 75% → UP
        List<Attendance> current = buildAttendance(STUDENT_UUID, "9A", FROM, 18, 2);
        List<Attendance> prev    = buildAttendance(STUDENT_UUID, "9A", FROM.minusDays(31), 15, 5);
        when(attendanceRepo.findByClassIdAndDateBetween(eq("9A"), any(), any()))
                .thenReturn(current).thenReturn(prev);

        assertThat(service.getClassSummary(CLASS_SEQ, FROM, TODAY, SCHOOL).get(0).trend()).isEqualTo("UP");
    }

    @Test
    void classSummary_trendIsDown() {
        when(classRoomRepo.findBySeqId(CLASS_SEQ)).thenReturn(Optional.of(room));
        when(policyRepo.findBySchoolNameIgnoreCase(SCHOOL)).thenReturn(Optional.of(policy));
        when(studentRepo.findByClassId(CLASS_UUID)).thenReturn(List.of(student));

        // current: 15/20 = 75%; prev: 18/20 = 90% → DOWN
        List<Attendance> current = buildAttendance(STUDENT_UUID, "9A", FROM, 15, 5);
        List<Attendance> prev    = buildAttendance(STUDENT_UUID, "9A", FROM.minusDays(31), 18, 2);
        when(attendanceRepo.findByClassIdAndDateBetween(eq("9A"), any(), any()))
                .thenReturn(current).thenReturn(prev);

        assertThat(service.getClassSummary(CLASS_SEQ, FROM, TODAY, SCHOOL).get(0).trend()).isEqualTo("DOWN");
    }

    @Test
    void classSummary_trendIsStable() {
        when(classRoomRepo.findBySeqId(CLASS_SEQ)).thenReturn(Optional.of(room));
        when(policyRepo.findBySchoolNameIgnoreCase(SCHOOL)).thenReturn(Optional.of(policy));
        when(studentRepo.findByClassId(CLASS_UUID)).thenReturn(List.of(student));

        // current: 16/20 = 80%; prev: 16/20 = 80% → STABLE
        List<Attendance> records = buildAttendance(STUDENT_UUID, "9A", FROM, 16, 4);
        when(attendanceRepo.findByClassIdAndDateBetween(eq("9A"), any(), any()))
                .thenReturn(records).thenReturn(records);

        assertThat(service.getClassSummary(CLASS_SEQ, FROM, TODAY, SCHOOL).get(0).trend()).isEqualTo("STABLE");
    }

    // ── School scope enforcement ──────────────────────────────────────────────

    @Test
    void classSummary_throwsForbiddenForOtherSchool() {
        ClassRoom otherSchoolRoom = new ClassRoom(CLASS_UUID, "Class 9", "A", "2025-26", null, "Other School", CLASS_SEQ);
        when(classRoomRepo.findBySeqId(CLASS_SEQ)).thenReturn(Optional.of(otherSchoolRoom));

        assertThatThrownBy(() -> service.getClassSummary(CLASS_SEQ, FROM, TODAY, SCHOOL))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Access denied");
    }

    // ── Anomaly acknowledge (idempotent) ─────────────────────────────────────

    @Test
    void acknowledgeAnomaly_isIdempotent() {
        AttendanceAnomaly anomaly = new AttendanceAnomaly();
        anomaly.setAcknowledgedAt(Instant.now()); // already acknowledged

        when(anomalyRepo.findById(1L)).thenReturn(Optional.of(anomaly));

        service.acknowledgeAnomaly(1L, 99L);

        verify(anomalyRepo, never()).save(any());
    }

    @Test
    void acknowledgeAnomaly_setsUserAndTimestamp() {
        AttendanceAnomaly anomaly = new AttendanceAnomaly();
        when(anomalyRepo.findById(2L)).thenReturn(Optional.of(anomaly));

        service.acknowledgeAnomaly(2L, 42L);

        verify(anomalyRepo).save(argThat(a ->
                a.getAcknowledgedByUserId().equals(42L) && a.getAcknowledgedAt() != null));
    }

    @Test
    void acknowledgeAnomaly_throwsWhenNotFound() {
        when(anomalyRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acknowledgeAnomaly(99L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Attendance> buildAttendance(UUID studentId, String classLabel,
                                              LocalDate startDate, int present, int absent) {
        List<Attendance> result = new java.util.ArrayList<>();
        LocalDate d = startDate;
        for (int i = 0; i < present; i++) {
            Attendance a = new Attendance();
            a.setStudentId(studentId);
            a.setClassId(classLabel);
            a.setDate(d);
            a.setStatus(AttendanceStatus.PRESENT);
            a.setMarkedBy(UUID.randomUUID());
            result.add(a);
            d = d.plusDays(1);
        }
        for (int i = 0; i < absent; i++) {
            Attendance a = new Attendance();
            a.setStudentId(studentId);
            a.setClassId(classLabel);
            a.setDate(d);
            a.setStatus(AttendanceStatus.ABSENT);
            a.setMarkedBy(UUID.randomUUID());
            result.add(a);
            d = d.plusDays(1);
        }
        return result;
    }
}
