package com.forgeStackk.EduResolve.notes.dto;

import java.time.Instant;
import java.util.List;

public record NoteDetailResponse(
    Long        id,
    Long        studentId,
    String      title,
    String      content,
    String      sourceType,
    String      language,
    Long        subjectId,
    String      subjectName,
    String      chapterRef,
    String      sourceFileName,
    Integer     sourcePageCount,
    String      aiModelUsed,
    boolean     isEdited,
    boolean     isPinned,
    boolean     isSharedToClassroom,
    Long        sharedClassroomId,
    List<String> tags,
    boolean     isActive,
    Instant     deletedAt,
    Instant     restoredAt,
    boolean     isArchived,
    Instant     createdAt,
    Instant     updatedAt
) {}
