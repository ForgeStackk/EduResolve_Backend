package com.forgeStackk.EduResolve.classroom.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ClassroomMessageDto(
    Long               id,
    Long               roomId,
    String             roomType,
    Long               senderId,
    String             senderName,
    String             senderRole,
    String             messageType,
    String             textContent,     // null if isDeleted
    String             attachmentUrl,
    String             attachmentType,
    String             attachmentName,
    NoteSharePreview   sharedNote,      // null unless messageType = NOTE_SHARE
    Long               replyToMessageId,
    boolean            isPinned,
    boolean            isDeleted,
    Map<String, Long>  reactions,       // emoji → count
    List<String>       myReactions,     // emojis the current user reacted with
    Instant            createdAt
) {
    public record NoteSharePreview(
        Long   noteId,
        String title,
        String language,
        String subjectName,
        String contentPreview
    ) {}
}
