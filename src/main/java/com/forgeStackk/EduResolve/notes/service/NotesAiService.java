package com.forgeStackk.EduResolve.notes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Separate AI service for note generation — uses configurable max-tokens
 * independent of the chat AI service (which is capped at 600 for chat responses).
 */
@Service
public class NotesAiService {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.api.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.notes.max-tokens:4000}")
    private int maxTokens;

    @Value("${openai.notes.timeout-seconds:120}")
    private int timeoutSeconds;

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public NotesAiService(WebClient.Builder builder) {
        this.webClient = builder
            .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
            .build();
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Streams raw token deltas from OpenAI.
     * Caller accumulates the full response for DB persistence.
     */
    public Flux<String> generateNoteStream(String systemPrompt, String userContent) {
        if (!isEnabled()) return Flux.just("[AI service is not configured. Please contact your administrator.]");

        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user",   "content", userContent)
        );

        Map<String, Object> body = new HashMap<>();
        body.put("model",       model);
        body.put("messages",    messages);
        body.put("max_tokens",  maxTokens);
        body.put("temperature", 0.7);
        body.put("stream",      true);

        return webClient.post()
            .uri(OPENAI_URL)
            .header("Authorization", "Bearer " + apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToFlux(String.class)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .filter(line -> line.startsWith("data: ") && !line.contains("[DONE]"))
            .map(line -> line.substring(6).trim())
            .flatMap(data -> {
                try {
                    String delta = mapper.readTree(data).at("/choices/0/delta/content").asText("");
                    return delta.isEmpty() ? Flux.empty() : Flux.just(delta);
                } catch (Exception e) {
                    return Flux.<String>empty();
                }
            })
            .onErrorResume(e -> {
                String msg = "\n\n[Generation interrupted: " + e.getMessage() + "]";
                return Flux.just(msg);
            });
    }
}
