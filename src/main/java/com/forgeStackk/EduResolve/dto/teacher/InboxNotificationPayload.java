package com.forgeStackk.EduResolve.dto.teacher;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InboxNotificationPayload {
    private UUID messageId;
    private String preview;
    private String senderName;
    private Instant sentAt;
}
