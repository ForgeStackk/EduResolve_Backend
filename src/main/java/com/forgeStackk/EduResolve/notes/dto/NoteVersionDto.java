package com.forgeStackk.EduResolve.notes.dto;

import java.time.Instant;

public record NoteVersionDto(
    Long    id,
    Integer versionNumber,
    String  language,
    Instant editedAt,
    String  contentSnapshot  // null in list view, populated in restore preview
) {}
