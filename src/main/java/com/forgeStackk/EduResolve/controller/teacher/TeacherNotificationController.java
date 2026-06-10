package com.forgeStackk.EduResolve.controller.teacher;

import com.forgeStackk.EduResolve.entity.teacher.TeacherNotification;
import com.forgeStackk.EduResolve.repository.teacher.TeacherNotificationRepository;
import com.forgeStackk.EduResolve.security.TeacherPortalAuthHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher-portal/notifications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TeacherNotificationController {

    private final TeacherNotificationRepository notificationRepo;
    private final TeacherPortalAuthHelper authHelper;

    // GET /notifications?page=0&size=20
    @GetMapping
    public ResponseEntity<Page<TeacherNotification>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID teacherId = authHelper.resolveTeacherId();
        return ResponseEntity.ok(
                notificationRepo.findByTeacherIdOrderByCreatedAtDesc(
                        teacherId, PageRequest.of(page, size)));
    }

    // GET /notifications/unread-count
    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount() {
        UUID teacherId = authHelper.resolveTeacherId();
        return ResponseEntity.ok(notificationRepo.countByTeacherIdAndIsRead(teacherId, false));
    }

    // PATCH /notifications/{id}/read
    @Transactional
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        notificationRepo.findById(id).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepo.save(n);
        });
        return ResponseEntity.ok().build();
    }
}
