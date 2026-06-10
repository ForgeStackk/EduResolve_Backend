package com.forgeStackk.EduResolve.classroom.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "classroom_pinned_messages",
    indexes = @Index(name = "idx_pinned_messages_room", columnList = "room_id, pinned_at DESC"))
@Data
@NoArgsConstructor
public class ClassroomPinnedMessage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "pinned_by_user_id", nullable = false)
    private Long pinnedByUserId;

    @Column(name = "pinned_at", nullable = false, updatable = false)
    private Instant pinnedAt;

    @PrePersist void onCreate() { pinnedAt = Instant.now(); }
}
