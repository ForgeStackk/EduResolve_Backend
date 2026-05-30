package com.forgeStackk.EduResolve.notes.dto;

public record GenerateNoteRequest(
    String sourceType,      // TOPIC_INPUT / CHAPTER / VOICE / PHOTO_OCR / PDF_UPLOAD / DOUBT_THREAD / MANUAL
    String language,        // "en" | "hi"  — required
    String prompt,
    Long   subjectId,
    Long   chapterId,
    String voiceFileUrl,
    String photoUrl,
    Long   pdfJobId,
    Long   doubtThreadId,
    String noteLength       // BRIEF | STANDARD | DETAILED — overrides preference for this note
) {}
