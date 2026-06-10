package com.forgeStackk.EduResolve.classroom.service;

import com.forgeStackk.EduResolve.classroom.dto.ClassroomEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClassroomWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(Long classroomId, String eventType, Object payload) {
        messagingTemplate.convertAndSend(
            "/topic/classroom/" + classroomId,
            new ClassroomEventPayload(eventType, payload)
        );
    }
}
