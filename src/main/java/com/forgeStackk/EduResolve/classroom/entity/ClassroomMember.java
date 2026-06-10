package com.forgeStackk.EduResolve.classroom.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "classroom_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"classroom_id", "user_id"}),
    indexes = @Index(name = "idx_classroom_members", columnList = "classroom_id, user_id"))
@Data
@NoArgsConstructor
public class ClassroomMember {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** STUDENT | MONITOR | TEACHER */
    @Column(nullable = false, length = 20)
    private String role = "STUDENT";

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "is_online", nullable = false)
    private boolean isOnline = false;

    @PrePersist void onCreate() { joinedAt = Instant.now(); }
}
