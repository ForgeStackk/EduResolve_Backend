package com.forgeStackk.EduResolve.classroom.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "classroom_reported_messages",
    indexes = @Index(name = "idx_reported_messages", columnList = "message_id"))
@Data
@NoArgsConstructor
public class ClassroomReportedMessage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "reported_by_user_id", nullable = false)
    private Long reportedByUserId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "reported_at", nullable = false, updatable = false)
    private Instant reportedAt;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @PrePersist void onCreate() { reportedAt = Instant.now(); }
}
