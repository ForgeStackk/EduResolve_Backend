package com.forgeStackk.EduResolve.classroom.dto;

public record SendMessageRequest(
    String roomType,            // GENERAL | SUBJECT
    String messageType,         // TEXT | VOICE | FILE | NOTE_SHARE
    String textContent,
    String attachmentUrl,
    String attachmentType,
    String attachmentName,
    Long   sharedNoteId,
    Long   replyToMessageId
) {}
