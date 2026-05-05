package com.forgeStackk.EduResolve.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Pluggable AI provider seam for the doubt solver. Default impl is a
 * deterministic local responder so the system runs with ZERO cloud cost.
 *
 * To enable a real provider:
 *   1. Set OPENAI_API_KEY env var.
 *   2. Replace `generateAnswer` with a real client call.
 *
 * The doubt solver always tries DB first; this is invoked only on miss.
 */
@Service
public class AiService {

    @Value("${openai.api-key:}")
    private String apiKey;

    /** True when a real AI key is configured. The controller uses this to attribute the source. */
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Returns a short, plain-text answer. When no API key is configured, falls
     * back to a deterministic stub so the UI never breaks.
     */
    public String generateAnswer(String question, String language) {
        if (!isEnabled()) {
            return ("hi".equalsIgnoreCase(language)
                ? "AI sahaayata abhi configure nahi hai. Apne shikshak se poochiye ya pustak ka uttar dekhiye."
                : "AI assistance is not configured. Please consult your teacher or textbook for a precise answer.");
        }
        // TODO: real OpenAI / Gemini / local LLM call here. Keep responses <= 80 words.
        return "[stub] AI provider configured but client not implemented.";
    }
}
