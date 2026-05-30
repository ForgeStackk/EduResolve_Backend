package com.forgeStackk.EduResolve.notes.dto;

public record PdfUploadResponse(
    Long   jobId,
    String fileName,
    String status
) {}
