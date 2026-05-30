package com.forgeStackk.EduResolve.notes.dto;

public record NotePreferenceDto(
    String preferredLanguage,    // "en" | "hi"
    String preferredNoteLength   // BRIEF | STANDARD | DETAILED
) {}
