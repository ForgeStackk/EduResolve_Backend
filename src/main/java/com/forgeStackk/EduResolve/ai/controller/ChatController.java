package com.forgeStackk.EduResolve.ai.controller;

import com.forgeStackk.EduResolve.ai.dto.ChatRequest;
import com.forgeStackk.EduResolve.ai.dto.ChatResponse;
import com.forgeStackk.EduResolve.ai.entity.AiConversation;
import com.forgeStackk.EduResolve.ai.service.ConversationService;
import com.forgeStackk.EduResolve.ai.service.OpenAiChatService;
import com.forgeStackk.EduResolve.ai.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * POST /api/ai/chat         — blocking, full reply
 * POST /api/ai/chat/stream  — SSE streaming, token-by-token
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ChatController {

    private final OpenAiChatService   openAiService;
    private final ConversationService conversationService;
    private final RateLimitService    rateLimitService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest req) {
        String rateKey = req.userId() != null ? "user:" + req.userId() : "anon";
        if (!rateLimitService.isAllowed(rateKey)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        if (req.message() == null || req.message().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        AiConversation conv = conversationService.getOrCreate(
            req.sessionId(), req.userId(), req.grade(), req.subject());

        String systemPrompt = buildSystemPrompt(req.grade(), req.subject(), req.language());
        List<Map<String, String>> messages = conversationService.buildContext(conv.getId(), systemPrompt);
        messages.add(Map.of("role", "user", "content", req.message()));

        String reply = openAiService.chat(messages);
        conversationService.saveExchange(conv.getId(), req.message(), reply);

        return ResponseEntity.ok(new ChatResponse(
            conv.getSessionId(), reply, "gpt-4o-mini", estimateTokens(reply), "AI"
        ));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@RequestBody ChatRequest req) {
        String rateKey = req.userId() != null ? "user:" + req.userId() : "anon";
        if (!rateLimitService.isAllowed(rateKey)) {
            return Flux.just(ServerSentEvent.<String>builder()
                .event("error").data("Rate limit exceeded. Try again in a minute.").build());
        }
        if (req.message() == null || req.message().isBlank()) {
            return Flux.just(ServerSentEvent.<String>builder()
                .event("error").data("Message cannot be empty.").build());
        }

        AiConversation conv = conversationService.getOrCreate(
            req.sessionId(), req.userId(), req.grade(), req.subject());

        String systemPrompt = buildSystemPrompt(req.grade(), req.subject(), req.language());
        List<Map<String, String>> messages = conversationService.buildContext(conv.getId(), systemPrompt);
        messages.add(Map.of("role", "user", "content", req.message()));

        StringBuilder fullReply = new StringBuilder();

        return openAiService.chatStream(messages)
            .map(delta -> {
                fullReply.append(delta);
                return ServerSentEvent.<String>builder().event("delta").data(delta).build();
            })
            .concatWith(Flux.defer(() -> {
                if (!fullReply.isEmpty()) {
                    conversationService.saveExchange(conv.getId(), req.message(), fullReply.toString());
                }
                return Flux.just(ServerSentEvent.<String>builder()
                    .event("done").data(conv.getSessionId()).build());
            }))
            .onErrorResume(e -> Flux.just(ServerSentEvent.<String>builder()
                .event("error").data("Stream interrupted. Please retry.").build()));
    }

    private String buildSystemPrompt(String grade, String subject, String language) {
        String gradeCtx   = grade   != null && !grade.isBlank()   ? "Class " + grade : "school";
        String subjectCtx = subject != null && !subject.isBlank() ? " — " + subject   : "";

        if ("hi".equalsIgnoreCase(language)) {
            return String.format("""
                आप %s%s के छात्रों के लिए एक अनुभवी भारतीय शिक्षक हैं।
                सरल हिंदी में 3-5 वाक्यों में उत्तर दें। NCERT पाठ्यक्रम का पालन करें।
                उदाहरण दें। प्रोत्साहित करें। केवल शिक्षा संबंधी विषयों पर उत्तर दें।
                """, gradeCtx, subjectCtx);
        }
        return String.format("""
            You are a friendly, experienced Indian tutor for %s students%s.
            Answer in 4-6 concise sentences using simple English.
            Follow NCERT curriculum. Give relatable examples.
            Be encouraging. Only answer education-related questions.
            Never mention you are an AI.
            """, gradeCtx, subjectCtx);
    }

    private static int estimateTokens(String text) {
        return text == null ? 0 : Math.max(1, text.length() / 4);
    }
}
