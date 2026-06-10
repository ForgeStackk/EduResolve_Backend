package com.forgeStackk.EduResolve.notes.dto;

import java.time.Instant;
import java.util.List;

public record NoteListItem(
    Long        id,
    String      title,
    Long        subjectId,
    String      subjectName,
    String      sourceType,
    String      language,
    String      sourceFileName,
    Integer     sourcePageCount,
    List<String> tags,
    boolean     isPinned,
    boolean     isSharedToClassroom,
    boolean     isEdited,
    Instant     createdAt,
    Instant     updatedAt,
    String      contentPreview,
    Instant     deletedAt          // non-null only in trash responses
) {}
