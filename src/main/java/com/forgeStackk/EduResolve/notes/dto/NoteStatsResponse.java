package com.forgeStackk.EduResolve.notes.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record NoteStatsResponse(
    long             totalNotes,
    List<SubjectCount> notesBySubject,
    Map<String, Long> notesByLanguage,
    long             notesGeneratedToday,
    long             dailyLimitRemaining,
    Instant          lastGeneratedAt
) {
    public record SubjectCount(String subjectName, long count) {}
}
