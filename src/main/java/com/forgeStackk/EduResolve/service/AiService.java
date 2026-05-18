package com.forgeStackk.EduResolve.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgeStackk.EduResolve.entity.QuestionBank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiService {

    private final RestClient restClient;
    private final String model;
    private final boolean enabled;

    public AiService(
            @Value("${openai.api.key:}") String apiKey,
            @Value("${openai.api.model:gpt-4o-mini}") String model) {
        this.model = model;
        this.enabled = apiKey != null && !apiKey.isBlank();
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String generateAnswer(String question, String language) {
        if (!enabled) {
            return "hi".equalsIgnoreCase(language)
                    ? "कृपया अपने पाठ्यपुस्तक से संदर्भ लें या शिक्षक से संपर्क करें।"
                    : "Please consult your teacher or textbook for a precise answer. AI assistance is not configured.";
        }

        try {
            String systemPrompt = buildSystemPrompt(language);

            var body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", question)
                    ),
                    "max_tokens", 300,
                    "temperature", 0.7
            );

            ChatResponse response = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(ChatResponse.class);

            if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                String answer = response.choices().get(0).message().content();
                return answer != null ? answer.trim() : getFallbackAnswer(language);
            }
            return getFallbackAnswer(language);

        } catch (Exception e) {
            log.error("OpenAI API error in AiService", e);
            return getFallbackAnswer(language);
        }
    }

    private String buildSystemPrompt(String language) {
        if ("hi".equalsIgnoreCase(language)) {
            return """
                    आप एक अनुभवी शिक्षक हैं जो छात्रों को शिक्षा संबंधी प्रश्नों का उत्तर देते हैं।
                    सरल और समझने में आसान भाषा का उपयोग करें। उत्तर संक्षिप्त रखें (3-4 वाक्य)। हिंदी में उत्तर दें।
                    """;
        }
        return """
                You are an experienced teacher helping students with educational questions.
                Use simple language. Keep the answer concise (3-4 sentences). Be encouraging and clear.
                Answer directly without any preamble.
                """;
    }

    private String getFallbackAnswer(String language) {
        return "hi".equalsIgnoreCase(language)
                ? "वर्तमान में AI उपलब्ध नहीं है। कृपया अपने पाठ्यपुस्तक से संदर्भ लें या शिक्षक से संपर्क करें।"
                : "AI is currently unavailable. Please refer to your textbook or consult your teacher.";
    }

    public List<QuestionBank> generateQuizQuestions(String chapterInfo, String difficulty, int count) {
        if (!enabled) return List.of();
        try {
            String prompt = String.format(
                "Generate %d NCERT-style multiple choice questions about: %s. Difficulty: %s.\n" +
                "Return ONLY a JSON array with no markdown. Each object must have exactly:\n" +
                "questionText, optionA, optionB, optionC, optionD, correctOption (A/B/C/D), explanation",
                count, chapterInfo, difficulty != null ? difficulty : "MEDIUM"
            );

            var body = Map.of(
                "model", model,
                "messages", List.of(
                    Map.of("role", "system", "content", "You generate quiz questions. Output pure JSON only, no markdown."),
                    Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", 2500,
                "temperature", 0.7
            );

            ChatResponse response = restClient.post()
                .uri("/chat/completions")
                .body(body)
                .retrieve()
                .body(ChatResponse.class);

            if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                String json = response.choices().get(0).message().content();
                return parseQuizJson(json, difficulty);
            }
        } catch (Exception e) {
            log.error("AI quiz generation failed", e);
        }
        return List.of();
    }

    private List<QuestionBank> parseQuizJson(String json, String difficulty) {
        try {
            String cleaned = json.strip()
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .strip();
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> raw = mapper.readValue(cleaned, new TypeReference<>() {});
            QuestionBank.Difficulty diff;
            try {
                diff = QuestionBank.Difficulty.valueOf(difficulty != null ? difficulty.toUpperCase() : "MEDIUM");
            } catch (Exception e) {
                diff = QuestionBank.Difficulty.MEDIUM;
            }
            final QuestionBank.Difficulty finalDiff = diff;
            return raw.stream().map(m -> {
                QuestionBank q = new QuestionBank();
                q.setQuestionText(str(m, "questionText"));
                q.setOptionA(str(m, "optionA"));
                q.setOptionB(str(m, "optionB"));
                q.setOptionC(str(m, "optionC"));
                q.setOptionD(str(m, "optionD"));
                q.setCorrectOption(str(m, "correctOption", "A"));
                q.setExplanation(str(m, "explanation"));
                q.setDifficulty(finalDiff);
                return q;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to parse AI quiz JSON: {}", json, e);
            return List.of();
        }
    }

    private String str(Map<String, Object> m, String key) {
        return str(m, key, "");
    }

    private String str(Map<String, Object> m, String key, String fallback) {
        Object v = m.get(key);
        return v != null ? v.toString() : fallback;
    }

    private record ChatResponse(List<Choice> choices) {
        private record Choice(Message message) {}
        private record Message(String role, String content) {}
    }
}
