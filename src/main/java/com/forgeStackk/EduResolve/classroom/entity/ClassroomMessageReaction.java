package com.forgeStackk.EduResolve.classroom.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "classroom_message_reactions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "user_id", "emoji"}),
    indexes = @Index(name = "idx_reactions_message", columnList = "message_id"))
@Data
@NoArgsConstructor
public class ClassroomMessageReaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 10)
    private String emoji;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }
}
