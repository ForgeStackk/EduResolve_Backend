package com.forgeStackk.EduResolve.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
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
 * Calls OpenAI /v1/chat/completions via WebClient.
 * Blocking path: chat()   — returns full answer string.
 * Streaming path: chatStream() — emits raw token-delta strings.
 */
@Service
public class OpenAiChatService {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final int    MAX_TOKENS = 600;
    private static final int    TIMEOUT_S  = 40;

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.api.model:gpt-4o-mini}")
    private String model;

    private final WebClient      webClient;
    private final ObjectMapper   mapper = new ObjectMapper();

    public OpenAiChatService(WebClient.Builder builder) {
        this.webClient = builder
            .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
            .build();
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Blocking — returns the complete assistant reply. */
    public String chat(List<Map<String, String>> messages) {
        if (!isEnabled()) return fallback();
        try {
            String json = webClient.post()
                .uri(OPENAI_URL)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body(messages, false))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(TIMEOUT_S))
                .retry(2)
                .block();

            JsonNode root = mapper.readTree(json);
            return root.at("/choices/0/message/content").asText(fallback()).trim();
        } catch (Exception e) {
            System.err.println("[OpenAiChatService] chat error: " + e.getMessage());
            return fallback();
        }
    }

    /**
     * Streaming — emits each token delta as it arrives.
     * The caller is responsible for accumulating the full reply for DB persistence.
     */
    public Flux<String> chatStream(List<Map<String, String>> messages) {
        if (!isEnabled()) return Flux.just(fallback());

        return webClient.post()
            .uri(OPENAI_URL)
            .header("Authorization", "Bearer " + apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body(messages, true))
            .retrieve()
            .bodyToFlux(String.class)
            .timeout(Duration.ofSeconds(60))
            .retry(2)
            .filter(line -> line.startsWith("data: ") && !line.contains("[DONE]"))
            .map(line -> line.substring(6).trim())
            .flatMap(data -> {
                try {
                    String delta = mapper.readTree(data).at("/choices/0/delta/content").asText("");
                    return delta.isEmpty() ? Flux.empty() : Flux.just(delta);
                } catch (Exception e) {
                    return Flux.empty();
                }
            })
            .onErrorResume(e -> {
                System.err.println("[OpenAiChatService] stream error: " + e.getMessage());
                return Flux.just(fallback());
            });
    }

    private Map<String, Object> body(List<Map<String, String>> messages, boolean stream) {
        Map<String, Object> b = new HashMap<>();
        b.put("model",       model);
        b.put("messages",    messages);
        b.put("max_tokens",  MAX_TOKENS);
        b.put("temperature", 0.7);
        b.put("stream",      stream);
        return b;
    }

    private String fallback() {
        return "AI is currently unavailable. Please consult your textbook or teacher.";
    }
}
