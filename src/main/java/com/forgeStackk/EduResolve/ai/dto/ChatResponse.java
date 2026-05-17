package com.forgeStackk.EduResolve.ai.dto;

public record ChatResponse(
    String sessionId,
    String reply,
    String model,
    int    tokensUsed,
    String source
) {}
