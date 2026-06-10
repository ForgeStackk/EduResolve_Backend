package com.forgeStackk.EduResolve.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OpenAIService {

    private final RestClient restClient;
    private final String model;

    public OpenAIService(
            @Value("${openai.api.key:}") String apiKey,
            @Value("${openai.api.model:gpt-4o-mini}") String model) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String generateClassAppropriateAnswer(String question, int classLevel, String subject, String context) {
        try {
            String systemPrompt = buildSystemPrompt(classLevel, subject, context);
            String userPrompt = buildUserPrompt(question, classLevel);
            ChatResponse response = callChatApi(systemPrompt, userPrompt, 600);
            String answer = response.choices().get(0).message().content();
            log.info("OpenAI answered for class {}: {}", classLevel,
                    question.substring(0, Math.min(50, question.length())));
            return answer;
        } catch (Exception e) {
            log.error("OpenAI text call failed", e);
            return getFallbackAnswer(question, classLevel);
        }
    }

    public String generateAnswerWithImage(byte[] imageBytes, String mimeType, String question,
                                          int classLevel, String subject) {
        try {
            String b64 = Base64.getEncoder().encodeToString(imageBytes);
            String dataUrl = "data:" + mimeType + ";base64," + b64;
            String systemPrompt = buildSystemPrompt(classLevel, subject, null);

            var userContent = List.of(
                    Map.of("type", "text", "text",
                            "I am a Class " + classLevel + " student. " +
                            (question.isBlank() ? "Please explain this image." : question)),
                    Map.of("type", "image_url",
                            "image_url", Map.of("url", dataUrl, "detail", "low"))
            );

            var body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userContent)
                    ),
                    "max_tokens", 800
            );

            ChatResponse response = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(ChatResponse.class);

            return response.choices().get(0).message().content();
        } catch (Exception e) {
            log.error("OpenAI vision call failed", e);
            return getFallbackAnswer(question.isBlank() ? "this image" : question, classLevel);
        }
    }

    public List<QuizQuestion> generateQuizQuestions(String chapterContent, int classLevel,
                                                     String difficulty, int count) {
        try {
            String systemMsg = "You are an expert NCERT educator who creates quiz questions.";
            String userMsg = buildQuizPrompt(chapterContent, classLevel, difficulty, count);
            ChatResponse response = callChatApi(systemMsg, userMsg, 1200);
            return parseQuizQuestions(response.choices().get(0).message().content());
        } catch (Exception e) {
            log.error("OpenAI quiz generation failed", e);
            return List.of();
        }
    }

    private ChatResponse callChatApi(String systemContent, String userContent, int maxTokens) {
        var body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemContent),
                        Map.of("role", "user", "content", userContent)
                ),
                "max_tokens", maxTokens,
                "temperature", 0.7
        );
        return restClient.post()
                .uri("/chat/completions")
                .body(body)
                .retrieve()
                .body(ChatResponse.class);
    }

    private String buildSystemPrompt(int classLevel, String subject, String context) {
        return String.format("""
                You are an expert NCERT educator for Class %d %s students.
                Use simple, age-appropriate language with relatable examples.
                Be concise, accurate, and encouraging.
                Context: %s
                """, classLevel,
                subject != null && !subject.isBlank() ? subject : "",
                context != null ? context : "General NCERT curriculum");
    }

    private String buildUserPrompt(String question, int classLevel) {
        return String.format("""
                Question: %s

                Answer suitable for a Class %d student. Include a clear explanation,
                a real-life example if useful, and key points to remember.
                """, question, classLevel);
    }

    private String buildQuizPrompt(String chapterContent, int classLevel, String difficulty, int count) {
        return String.format("""
                For Class %d, create %d MCQ questions (%s difficulty).
                Content: %s

                Format:
                Q: [question]
                A) ... B) ... C) ... D) ...
                Correct: [A/B/C/D]
                Explanation: [brief]
                """, classLevel, count, difficulty, chapterContent);
    }

    private String getFallbackAnswer(String question, int classLevel) {
        return String.format("""
                I'm having trouble connecting right now. For Class %d, please:
                1. Check your NCERT textbook for this topic
                2. Ask your teacher for clarification
                3. Try again in a few moments

                Your question: %s
                """, classLevel, question);
    }

    private List<QuizQuestion> parseQuizQuestions(String response) {
        return List.of();
    }

    // ---------- Response records ----------

    public record ChatResponse(List<Choice> choices) {
        public record Choice(Message message) {}
        public record Message(String role, String content) {}
    }

    @Data
    public static class QuizQuestion {
        private String question;
        private String optionA, optionB, optionC, optionD;
        private String correct;
        private String explanation;
    }

    @Data
    public static class VisualizationData {
        private String concept;
        private String type;
        private String content;
        private String description;
    }
}
