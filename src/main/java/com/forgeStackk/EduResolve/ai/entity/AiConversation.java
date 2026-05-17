package com.forgeStackk.EduResolve.ai.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "ai_conversation", indexes = {
    @Index(name = "idx_ai_conv_session", columnList = "session_id", unique = true),
    @Index(name = "idx_ai_conv_user",    columnList = "user_id")
})
@Data
@NoArgsConstructor
public class AiConversation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 64)
    private String sessionId;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 10)
    private String grade;

    @Column(length = 100)
    private String subject;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "total_tokens")
    private Integer totalTokens = 0;

    @Column(name = "message_count")
    private Integer messageCount = 0;

    @PrePersist  void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate   void onUpdate() { updatedAt = Instant.now(); }
}
