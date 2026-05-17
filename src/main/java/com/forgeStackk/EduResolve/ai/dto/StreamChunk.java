package com.forgeStackk.EduResolve.ai.dto;

public record StreamChunk(
    String  sessionId,
    String  delta,
    boolean done,
    String  error
) {}
