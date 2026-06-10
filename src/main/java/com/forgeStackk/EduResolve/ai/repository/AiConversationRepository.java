package com.forgeStackk.EduResolve.ai.repository;

import com.forgeStackk.EduResolve.ai.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    Optional<AiConversation> findBySessionId(String sessionId);
}
