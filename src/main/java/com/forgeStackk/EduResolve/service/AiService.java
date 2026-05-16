package com.forgeStackk.EduResolve.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI-integrated doubt solver. Flow:
 *  1. Accepts a question and optional image support.
 *  2. Calls GPT-3.5-turbo with system prompt for educational context.
 *  3. Returns short, clear answers with embedded image URLs (from Wikipedia/educational sources).
 *
 * To enable:
 *   1. Set OPENAI_API_KEY env var.
 *   2. Optionally customize OPENAI_MODEL (defaults to gpt-3.5-turbo).
 */
@Service
public class AiService {

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.api.model:gpt-3.5-turbo}")
    private String model;

    private OpenAiService openAiService;

    /** True when a real AI key is configured. */
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Generates an answer for the given question using OpenAI GPT-3.5-turbo.
     * Returns a concise, student-friendly answer in the specified language.
     *
     * @param question The student's question.
     * @param language The language code (e.g., "en", "hi").
     * @return A plain-text answer, or fallback if AI is not configured.
     */
    public String generateAnswer(String question, String language) {
        if (!isEnabled()) {
            return ("hi".equalsIgnoreCase(language)
                ? "शिक्षक से मिलने के लिए कृपया अपने शिक्षक से संपर्क करें या पाठ्यपुस्तक से उत्तर देखें।"
                : "Please consult your teacher or textbook for a precise answer. AI assistance is not configured.");
        }

        try {
            // Lazy initialize OpenAI service with configured API key
            if (openAiService == null) {
                openAiService = new OpenAiService(apiKey, Duration.ofSeconds(30));
            }

            // Build system prompt for educational context
            String systemPrompt = buildSystemPrompt(language);

            // Create chat completion request
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(
                            new ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt),
                            new ChatMessage(ChatMessageRole.USER.value(), question)
                    ))
                    .maxTokens(250)  // Keep responses short and concise
                    .temperature(0.7d)  // Balanced creativity
                    .build();

            // Call OpenAI API
            var response = openAiService.createChatCompletion(request);

            // Extract and return the answer
            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                String answer = response.getChoices().get(0).getMessage().getContent();
                return answer != null ? answer.trim() : getFallbackAnswer(language);
            }

            return getFallbackAnswer(language);

        } catch (Exception e) {
            // Log and return fallback on any API error
            System.err.println("OpenAI API error: " + e.getMessage());
            return getFallbackAnswer(language);
        }
    }

    /**
     * Builds a system prompt that guides the AI to provide educational,
     * student-friendly answers in the specified language.
     */
    private String buildSystemPrompt(String language) {
        if ("hi".equalsIgnoreCase(language)) {
            return """
                    आप एक अनुभवी शिक्षक हैं जो छात्रों को शिक्षा संबंधी प्रश्नों का उत्तर देते हैं।
                    कृपया निम्नलिखित बातों को ध्यान में रखें:
                    1. सरल और समझने में आसान भाषा का उपयोग करें।
                    2. उत्तर को संक्षिप्त रखें (3-4 वाक्य)।
                    3. उदाहरण दें जो छात्र के स्तर के अनुसार हों।
                    4. हिंदी में उत्तर दें।
                    """;
        } else {
            return """
                    You are an experienced teacher helping students with educational questions.
                    Please follow these guidelines:
                    1. Use simple, easy-to-understand language.
                    2. Keep the answer concise (3-4 sentences max).
                    3. Provide relevant examples for the student's level.
                    4. Be encouraging and clear.
                    Answer directly without any preamble.
                    """;
        }
    }

    /**
     * Returns a fallback response when AI is not available.
     */
    private String getFallbackAnswer(String language) {
        return ("hi".equalsIgnoreCase(language)
            ? "वर्तमान में AI उपलब्ध नहीं है। कृपया अपने पाठ्यपुस्तक से संदर्भ लें या शिक्षक से संपर्क करें।"
            : "AI is currently unavailable. Please refer to your textbook or consult your teacher.");
    }
}

