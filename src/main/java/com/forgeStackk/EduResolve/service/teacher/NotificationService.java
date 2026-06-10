package com.forgeStackk.EduResolve.service.teacher;

import com.forgeStackk.EduResolve.entity.teacher.TeacherNotification;
import com.forgeStackk.EduResolve.repository.teacher.TeacherNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final TeacherNotificationRepository notificationRepo;
    private final SimpMessagingTemplate ws;

    /**
     * Persists a notification for {@code teacherId} and pushes it over WebSocket.
     * WebSocket failure is non-fatal — the DB row is the source of truth.
     */
    @Transactional
    public void push(UUID teacherId, String message) {
        TeacherNotification notification = new TeacherNotification();
        notification.setTeacherId(teacherId);
        notification.setMessage(message);
        TeacherNotification saved = notificationRepo.save(notification);

        try {
            String destination = "/topic/notification/" + teacherId;
            Object payload = Map.of(
                            "notificationId", saved.getNotificationId().toString(),
                            "message", message,
                            "createdAt", saved.getCreatedAt().toString()
            );
            ws.convertAndSend(destination, payload);
        } catch (Exception e) {
            log.warn("WebSocket push failed for teacherId={}: {}", teacherId, e.getMessage());
        }
    }
}
