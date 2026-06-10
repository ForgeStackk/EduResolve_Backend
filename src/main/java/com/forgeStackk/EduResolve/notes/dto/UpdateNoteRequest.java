package com.forgeStackk.EduResolve.notes.dto;

import java.util.List;

public record UpdateNoteRequest(
    String       title,
    String       content,
    List<String> tags,
    Boolean      isPinned
) {}
