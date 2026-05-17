package com.forgeStackk.EduResolve.ai.repository;

import com.forgeStackk.EduResolve.ai.entity.AiConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AiConversationMessageRepository extends JpaRepository<AiConversationMessage, Long> {
    List<AiConversationMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
