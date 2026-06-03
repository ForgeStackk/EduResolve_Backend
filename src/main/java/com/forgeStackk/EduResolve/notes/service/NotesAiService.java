package com.forgeStackk.EduResolve.notes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
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
            .onErrorMap(e -> {
                log.error("OpenAI generation error: {}", e.getMessage());
                return new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY, "GENERATION_FAILED");
            });
    }

    /**
     * Transcribes audio using OpenAI Whisper (same API key, different endpoint).
     * Returns the transcript, or empty string on failure.
     * Intended to chain before generateNoteStream() in the audio-based notes flow.
     */
    public Mono<String> transcribeAudio(byte[] audioBytes, String filename, String mimeType) {
        if (!isEnabled()) return Mono.just("");

        org.springframework.http.client.MultipartBodyBuilder builder =
            new org.springframework.http.client.MultipartBodyBuilder();
        builder.part("model", "whisper-1");
        builder.part("file",
            new org.springframework.core.io.ByteArrayResource(audioBytes) {
                @Override public String getFilename() { return filename; }
            }).contentType(org.springframework.http.MediaType.parseMediaType(mimeType));

        return webClient.post()
            .uri("https://api.openai.com/v1/audio/transcriptions")
            .header("Authorization", "Bearer " + apiKey)
            .body(org.springframework.web.reactive.function.BodyInserters
                .fromMultipartData(builder.build()))
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(60))
            .map(json -> {
                try { return mapper.readTree(json).at("/text").asText("").trim(); }
                catch (Exception e) { return ""; }
            })
            .onErrorReturn("");
    }

    /**
     * Non-streaming vision call: extracts readable text from an image using gpt-4o-mini.
     * Returns the extracted text, or empty string on failure.
     * Intended to be chained before generateNoteStream() in the image-based notes flow.
     */
    public Mono<String> extractTextFromImage(byte[] imageBytes, String mimeType) {
        if (!isEnabled()) return Mono.just("");

        String dataUrl = "data:" + mimeType + ";base64,"
                       + Base64.getEncoder().encodeToString(imageBytes);

        List<Object> userContent = List.of(
            Map.of("type", "text", "text",
                   "Extract all readable text from this image exactly as it appears. " +
                   "If it is a handwritten note, textbook page, or printed document, " +
                   "transcribe the content verbatim. Return only the extracted text — no commentary."),
            Map.of("type", "image_url",
                   "image_url", Map.of("url", dataUrl, "detail", "high"))
        );

        Map<String, Object> body = new HashMap<>();
        body.put("model",       model);  // gpt-4o-mini supports vision
        body.put("messages",    List.of(Map.of("role", "user", "content", userContent)));
        body.put("max_tokens",  2000);
        body.put("temperature", 0.1);    // low temperature for faithful transcription

        return webClient.post()
            .uri(OPENAI_URL)
            .header("Authorization", "Bearer " + apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(30))
            .map(json -> {
                try {
                    return mapper.readTree(json).at("/choices/0/message/content").asText("").trim();
                } catch (Exception e) {
                    return "";
                }
            })
            .onErrorReturn("");
    }
}
