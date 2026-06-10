package com.forgeStackk.EduResolve.dto.teacher;

import com.forgeStackk.EduResolve.enums.MessageContentType;
import com.forgeStackk.EduResolve.enums.RecipientType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
public class MessageSummaryResponse {
    private UUID messageId;
    private RecipientType recipientType;
    private UUID targetClassId;
    private String textBody;
    private MessageContentType contentType;
    private Instant sentAt;
    private Boolean isHomework;
    private LocalDate homeworkDueDate;
    private int attachmentCount;
    private long readCount;
}
