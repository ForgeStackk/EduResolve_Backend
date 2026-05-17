package com.forgeStackk.EduResolve.ai.service;

import com.forgeStackk.EduResolve.ai.entity.AiConversation;
import com.forgeStackk.EduResolve.ai.entity.AiConversationMessage;
import com.forgeStackk.EduResolve.ai.repository.AiConversationMessageRepository;
import com.forgeStackk.EduResolve.ai.repository.AiConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final int MAX_CONTEXT_TOKENS = 6000;

    private final AiConversationRepository      convRepo;
    private final AiConversationMessageRepository msgRepo;

    @Transactional
    public AiConversation getOrCreate(String sessionId, Long userId, String grade, String subject) {
        String sid = (sessionId != null && !sessionId.isBlank())
            ? sessionId : UUID.randomUUID().toString();
        return convRepo.findBySessionId(sid)
            .orElseGet(() -> {
                AiConversation c = new AiConversation();
                c.setSessionId(sid);
                c.setUserId(userId);
                c.setGrade(grade);
                c.setSubject(subject);
                return convRepo.save(c);
            });
    }

    /**
     * Builds the message list for the OpenAI API call.
     * Slides a window over history so total tokens stay under MAX_CONTEXT_TOKENS.
     */
    @Transactional(readOnly = true)
    public List<Map<String, String>> buildContext(Long conversationId, String systemPrompt) {
        List<Map<String, String>> result = new ArrayList<>();
        result.add(Map.of("role", "system", "content", systemPrompt));

        List<AiConversationMessage> history =
            msgRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);

        int budget = MAX_CONTEXT_TOKENS - estimateTokens(systemPrompt);
        List<AiConversationMessage> window = new ArrayList<>();
        for (int i = history.size() - 1; i >= 0 && budget > 0; i--) {
            AiConversationMessage m = history.get(i);
            int cost = estimateTokens(m.getContent());
            if (cost > budget) break;
            budget -= cost;
            window.add(0, m);
        }

        for (AiConversationMessage m : window) {
            result.add(Map.of("role", m.getRole(), "content", m.getContent()));
        }
        return result;
    }

    @Transactional
    public void saveExchange(Long conversationId, String userMsg, String assistantMsg) {
        persist(conversationId, "user",      userMsg);
        persist(conversationId, "assistant", assistantMsg);
        convRepo.findById(conversationId).ifPresent(c -> {
            c.setMessageCount(c.getMessageCount() + 2);
            c.setTotalTokens(c.getTotalTokens()
                + estimateTokens(userMsg) + estimateTokens(assistantMsg));
            convRepo.save(c);
        });
    }

    private void persist(Long conversationId, String role, String content) {
        AiConversationMessage m = new AiConversationMessage();
        m.setConversationId(conversationId);
        m.setRole(role);
        m.setContent(content);
        m.setTokenCount(estimateTokens(content));
        msgRepo.save(m);
    }

    private static int estimateTokens(String text) {
        return text == null ? 0 : Math.max(1, text.length() / 4);
    }
}
