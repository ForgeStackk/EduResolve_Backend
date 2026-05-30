package com.forgeStackk.EduResolve.notes.dto;

public record PdfStatusResponse(
    Long    jobId,
    String  status,
    Integer pageCount,
    Integer characterCount,
    String  failureReason
) {}
