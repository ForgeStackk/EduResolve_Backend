package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "broadcast")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Broadcast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sent_by_id")
    private Long sentById;

    @Column(name = "sent_by_name", length = 255)
    private String sentByName;

    @Column(nullable = false, length = 255)
    private String channels;

    @Column(name = "audience_grades", length = 255)
    private String audienceGrades;

    @Column(name = "class_id")
    private java.util.UUID classId;

    @Column(name = "target_students", nullable = false, columnDefinition = "boolean not null default true")
    private boolean targetStudents = true;

    @Column(name = "target_parents", nullable = false, columnDefinition = "boolean not null default false")
    private boolean targetParents = false;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_emergency", nullable = false)
    private boolean isEmergency = false;

    @Column(nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "recipient_count", nullable = false)
    private int recipientCount = 0;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }
}
