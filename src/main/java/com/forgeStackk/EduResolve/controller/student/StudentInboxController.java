package com.forgeStackk.EduResolve.controller.student;

import com.forgeStackk.EduResolve.dto.student.AttendanceDayDto;
import com.forgeStackk.EduResolve.dto.student.StudentInboxItemDto;
import com.forgeStackk.EduResolve.enums.MessageCategory;
import com.forgeStackk.EduResolve.security.StudentPortalAuthHelper;
import com.forgeStackk.EduResolve.service.student.StudentPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student-portal")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StudentInboxController {

    private final StudentPortalService studentPortalService;
    private final StudentPortalAuthHelper authHelper;

    @GetMapping("/inbox")
    public ResponseEntity<List<StudentInboxItemDto>> getInbox(
            @RequestParam(required = false) MessageCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID studentId = authHelper.resolveStudentId();
        return ResponseEntity.ok(studentPortalService.getInbox(studentId, category, page, size));
    }

    @PatchMapping("/inbox/{inboxId}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID inboxId) {
        UUID studentId = authHelper.resolveStudentId();
        studentPortalService.markRead(studentId, inboxId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inbox/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        UUID studentId = authHelper.resolveStudentId();
        long count = studentPortalService.getUnreadCount(studentId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/attendance")
    public ResponseEntity<List<AttendanceDayDto>> attendance(
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(defaultValue = "0") int year) {
        UUID studentId = authHelper.resolveStudentId();
        int m = month > 0 ? month : LocalDate.now().getMonthValue();
        int y = year > 0 ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(studentPortalService.getAttendance(studentId, m, y));
    }
}
