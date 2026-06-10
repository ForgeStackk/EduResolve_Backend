package com.forgeStackk.EduResolve.service.admin;

import com.forgeStackk.EduResolve.entity.*;
import com.forgeStackk.EduResolve.entity.teacher.*;
import com.forgeStackk.EduResolve.enums.AttendanceStatus;
import com.forgeStackk.EduResolve.enums.ReasonCode;
import com.forgeStackk.EduResolve.repository.*;
import com.forgeStackk.EduResolve.repository.teacher.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AtRiskScanService {

    private final AttendanceRepository        attendanceRepo;
    private final ClassRoomRepository         classRoomRepo;
    private final StudentRepository           studentRepo;
    private final AtRiskSnapshotRepository    snapshotRepo;
    private final AttendancePolicyRepository  policyRepo;
    private final AttendanceAnomalyRepository anomalyRepo;
    private final BroadcastRepository         broadcastRepo;
    private final ParentInboxRepository       parentInboxRepo;
    private final ScheduledJobLogRepository   jobLogRepo;

    // Self-reference so @Transactional on scanSchool works when called internally
    @Autowired @Lazy
    private AtRiskScanService self;

    // ── Scheduled entry point ─────────────────────────────────────────────────

    @Scheduled(cron = "${attendance.scan.cron}")
    public void runScheduledScan() {
        log.info("At-risk scan starting");

        List<String> schools = classRoomRepo.findAll().stream()
                .map(ClassRoom::getSchoolName)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .toList();

        int totalScanned = 0, totalAtRisk = 0, totalNotified = 0;
        StringBuilder errors = new StringBuilder();

        for (String school : schools) {
            try {
                ScanResult r = self.scanSchool(school);
                totalScanned  += r.scanned();
                totalAtRisk   += r.atRisk();
                totalNotified += r.notified();
            } catch (Exception e) {
                log.error("At-risk scan failed for school '{}'", school, e);
                errors.append(school).append(": ").append(e.getMessage()).append('\n');
            }
        }

        ScheduledJobLog entry = new ScheduledJobLog();
        entry.setJobName("at-risk-scan");
        entry.setStudentsScanned(totalScanned);
        entry.setAtRiskFound(totalAtRisk);
        entry.setNotificationsSent(totalNotified);
        entry.setErrors(errors.length() > 0 ? errors.toString() : null);
        jobLogRepo.save(entry);

        log.info("At-risk scan done — scanned={}, atRisk={}, notified={}",
                totalScanned, totalAtRisk, totalNotified);
    }

    // ── Per-school scan (also called by manual trigger endpoint) ──────────────

    @Transactional
    public ScanResult scanSchool(String schoolName) {
        LocalDate today     = LocalDate.now();
        LocalDate currFrom  = today.minusDays(29);
        LocalDate prevTo    = currFrom.minusDays(1);
        LocalDate prevFrom  = prevTo.minusDays(29);

        List<ClassRoom> rooms = classRoomRepo.findBySchoolNameIgnoreCase(schoolName);
        AttendancePolicy policy = policyRepo.findBySchoolNameIgnoreCase(schoolName)
                .orElseGet(() -> defaultPolicy(schoolName));

        int scanned = 0, atRisk = 0, notified = 0;

        for (ClassRoom room : rooms) {
            String label = labelOf(room);
            List<Student> students = studentRepo.findByClassId(room.getClassId());
            if (students.isEmpty()) continue;

            List<Attendance> currRecs = attendanceRepo.findByClassIdAndDateBetween(label, currFrom, today);
            List<Attendance> prevRecs = attendanceRepo.findByClassIdAndDateBetween(label, prevFrom, prevTo);

            detectAnomaly(label, currRecs, prevRecs, students.size(), today);

            int wdCurr = distinctWorkingDays(currRecs);
            int wdPrev = distinctWorkingDays(prevRecs);
            Map<UUID, List<Attendance>> byCurr = groupByStudent(currRecs);
            Map<UUID, List<Attendance>> byPrev  = groupByStudent(prevRecs);

            List<Student> toNotify = new ArrayList<>();

            for (Student s : students) {
                scanned++;
                List<Attendance> sRec  = byCurr.getOrDefault(s.getStudentId(), List.of());
                List<Attendance> sPrev = byPrev.getOrDefault(s.getStudentId(), List.of());

                double currPct = pct(count(sRec, AttendanceStatus.PRESENT)
                        + count(sRec, AttendanceStatus.LATE), countExcused(sRec), wdCurr);
                double prevPct = pct(count(sPrev, AttendanceStatus.PRESENT)
                        + count(sPrev, AttendanceStatus.LATE), countExcused(sPrev), wdPrev);

                boolean isAtRisk = currPct < policy.getMinAttendancePct()
                        || (prevPct - currPct) >= policy.getAtRiskDropPct();
                if (!isAtRisk) continue;

                atRisk++;
                upsertSnapshot(s.getSeqId(), today, currPct, prevPct, dominantReason(sRec));

                if (policy.isAutoNotifyParents()
                        && currPct <= policy.getAutoNotifyThresholdPct()
                        && s.getParentId() != null
                        && isPastCooldown(s.getSeqId(), today, policy.getAutoNotifyCooldownDays())) {
                    toNotify.add(s);
                }
            }

            if (!toNotify.isEmpty()) {
                notified += fanOutParentNotifications(toNotify, label, schoolName, today);
            }
        }

        if (atRisk > 0) saveSummaryBroadcast(schoolName, scanned, atRisk, notified);

        return new ScanResult(scanned, atRisk, notified);
    }

    // ── Anomaly detection ─────────────────────────────────────────────────────

    private void detectAnomaly(String classLabel, List<Attendance> curr, List<Attendance> prev,
                               int numStudents, LocalDate today) {
        if (!anomalyRepo.findByClassIdAndDetectedDate(classLabel, today).isEmpty()) return;

        int wdCurr = distinctWorkingDays(curr);
        int wdPrev = distinctWorkingDays(prev);
        if (wdCurr == 0 || wdPrev == 0 || numStudents == 0) return;

        long presentCurr = curr.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT
                          || a.getStatus() == AttendanceStatus.LATE)
                .count();
        long presentPrev = prev.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT
                          || a.getStatus() == AttendanceStatus.LATE)
                .count();

        double currAvg = presentCurr * 100.0 / ((long) numStudents * wdCurr);
        double prevAvg = presentPrev * 100.0 / ((long) numStudents * wdPrev);
        double drop    = prevAvg - currAvg;

        if (drop < 25.0) return;

        AttendanceAnomaly anomaly = new AttendanceAnomaly();
        anomaly.setClassId(classLabel);
        anomaly.setDetectedDate(today);
        anomaly.setClassAvgPct(bd2(currAvg));
        anomaly.setExpectedPct(bd2(prevAvg));
        anomaly.setDropAmount(bd2(drop));
        anomalyRepo.save(anomaly);

        log.warn("Anomaly detected: class {} attendance dropped {}pp ({}% → {}%)",
                classLabel,
                String.format("%.1f", drop),
                String.format("%.1f", prevAvg),
                String.format("%.1f", currAvg));
    }

    // ── Snapshot upsert ───────────────────────────────────────────────────────

    private void upsertSnapshot(Long studentSeqId, LocalDate today,
                                double currPct, double prevPct, String reason) {
        if (snapshotRepo.findByStudentSeqIdAndSnapshotDate(studentSeqId, today).isPresent()) return;
        AtRiskSnapshot snap = new AtRiskSnapshot();
        snap.setStudentSeqId(studentSeqId);
        snap.setSnapshotDate(today);
        snap.setAttendancePct(bd2(currPct));
        snap.setPreviousPct(bd2(prevPct));
        snap.setReasonDominant(reason);
        snapshotRepo.save(snap);
    }

    // ── Cooldown check ────────────────────────────────────────────────────────

    private boolean isPastCooldown(Long studentSeqId, LocalDate today, int cooldownDays) {
        return snapshotRepo.findTopByStudentSeqIdOrderBySnapshotDateDesc(studentSeqId)
                .map(s -> s.getNotifiedAt() == null
                        || s.getNotifiedAt().isBefore(
                                today.minusDays(cooldownDays - 1L).atStartOfDay().toInstant(ZoneOffset.UTC)))
                .orElse(true);
    }

    // ── Parent notification fan-out ───────────────────────────────────────────

    private int fanOutParentNotifications(List<Student> students, String classLabel,
                                          String schoolName, LocalDate today) {
        Broadcast b = new Broadcast();
        b.setChannels("whatsapp");
        b.setTargetStudents(false);
        b.setTargetParents(true);
        b.setMessage("Dear Parent, your child in class " + classLabel + " at " + schoolName
                + " has been identified as at-risk due to low or declining attendance."
                + " Please ensure regular school attendance. Contact the school for details.");
        b.setSentByName("EduResolve System");
        b.setStatus("sent");
        b.setRecipientCount(students.size());
        b.setSentAt(Instant.now());
        broadcastRepo.save(b);

        Instant now = Instant.now();
        students.forEach(s -> {
            ParentInbox inbox = new ParentInbox();
            inbox.setParentId(s.getParentId());
            inbox.setBroadcastId(b.getId());
            parentInboxRepo.save(inbox);

            snapshotRepo.findByStudentSeqIdAndSnapshotDate(s.getSeqId(), today)
                    .ifPresent(snap -> { snap.setNotifiedAt(now); snapshotRepo.save(snap); });
        });

        return students.size();
    }

    // ── Admin summary broadcast ───────────────────────────────────────────────

    private void saveSummaryBroadcast(String schoolName, int scanned, int atRisk, int notified) {
        Broadcast b = new Broadcast();
        b.setChannels("in-app");
        b.setTargetStudents(false);
        b.setTargetParents(false);
        b.setMessage(String.format(
                "[System] At-risk scan for %s: %d students scanned, %d at-risk, %d parents notified.",
                schoolName, scanned, atRisk, notified));
        b.setSentByName("EduResolve System");
        b.setStatus("sent");
        b.setRecipientCount(0);
        b.setSentAt(Instant.now());
        broadcastRepo.save(b);
    }

    // ── Static helpers ────────────────────────────────────────────────────────

    static String labelOf(ClassRoom room) {
        return (room.getClassName().replace("Class ", "") + room.getSection()).toUpperCase();
    }

    private static Map<UUID, List<Attendance>> groupByStudent(List<Attendance> recs) {
        return recs.stream().collect(Collectors.groupingBy(Attendance::getStudentId));
    }

    private static int distinctWorkingDays(List<Attendance> recs) {
        return (int) recs.stream()
                .filter(a -> a.getStatus() != AttendanceStatus.HOLIDAY)
                .map(Attendance::getDate)
                .distinct().count();
    }

    private static int count(List<Attendance> recs, AttendanceStatus status) {
        return (int) recs.stream().filter(a -> a.getStatus() == status).count();
    }

    private static int countExcused(List<Attendance> recs) {
        return (int) recs.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT
                        && (a.getReasonCode() == ReasonCode.LEAVE
                         || a.getReasonCode() == ReasonCode.MEDICAL))
                .count();
    }

    private static double pct(int attended, int excused, int totalDays) {
        return totalDays > 0 ? Math.min(100.0, (attended + excused) * 100.0 / totalDays) : 100.0;
    }

    private static String dominantReason(List<Attendance> recs) {
        return recs.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT && a.getReasonCode() != null)
                .collect(Collectors.groupingBy(a -> a.getReasonCode().name(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private static BigDecimal bd2(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
    }

    private static AttendancePolicy defaultPolicy(String schoolName) {
        AttendancePolicy p = new AttendancePolicy();
        p.setSchoolName(schoolName);
        p.setMinAttendancePct(75);
        p.setAtRiskDropPct(10);
        p.setAutoNotifyParents(false);
        p.setAutoNotifyThresholdPct(75);
        p.setAutoNotifyCooldownDays(7);
        return p;
    }

    // ── Result type ───────────────────────────────────────────────────────────

    public record ScanResult(int scanned, int atRisk, int notified) {}
}
