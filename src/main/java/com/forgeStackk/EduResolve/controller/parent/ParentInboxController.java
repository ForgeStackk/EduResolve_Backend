package com.forgeStackk.EduResolve.controller.parent;

import com.forgeStackk.EduResolve.dto.student.AttendanceDayDto;
import com.forgeStackk.EduResolve.dto.student.StudentInboxItemDto;
import com.forgeStackk.EduResolve.enums.MessageCategory;
import com.forgeStackk.EduResolve.security.ParentPortalAuthHelper;
import com.forgeStackk.EduResolve.service.parent.ParentPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parent-portal")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ParentInboxController {

    private final ParentPortalService parentPortalService;
    private final ParentPortalAuthHelper authHelper;

    @GetMapping("/inbox")
    public ResponseEntity<List<StudentInboxItemDto>> getInbox(
            @RequestParam(required = false) MessageCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID parentId = authHelper.resolveParentId();
        return ResponseEntity.ok(parentPortalService.getInbox(parentId, category, page, size));
    }

    @PatchMapping("/inbox/{inboxId}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID inboxId) {
        UUID parentId = authHelper.resolveParentId();
        parentPortalService.markRead(parentId, inboxId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inbox/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        UUID parentId = authHelper.resolveParentId();
        long count = parentPortalService.getUnreadCount(parentId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/attendance")
    public ResponseEntity<List<AttendanceDayDto>> attendance(
            @RequestParam(required = false) UUID childId,
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(defaultValue = "0") int year) {
        UUID parentId = authHelper.resolveParentId();
        int m = month > 0 ? month : LocalDate.now().getMonthValue();
        int y = year > 0 ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(parentPortalService.getChildAttendance(parentId, childId, m, y));
    }
}
