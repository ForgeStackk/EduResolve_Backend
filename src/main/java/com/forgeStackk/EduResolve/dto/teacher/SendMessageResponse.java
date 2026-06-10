package com.forgeStackk.EduResolve.dto.teacher;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
public class SendMessageResponse {
    private UUID messageId;
    private Instant sentAt;
    private int deliveredTo;
}
