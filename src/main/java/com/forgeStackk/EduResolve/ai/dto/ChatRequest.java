package com.forgeStackk.EduResolve.ai.dto;

public record ChatRequest(
    String sessionId,
    Long   userId,
    String message,
    String grade,
    String subject,
    String language,
    boolean stream
) {}
