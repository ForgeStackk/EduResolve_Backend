package com.forgeStackk.EduResolve.entity.teacher;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "teacher_notification", indexes = {
        @Index(name = "idx_tn_teacher_id", columnList = "teacher_id"),
        @Index(name = "idx_tn_read_status", columnList = "teacher_id, is_read")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id")
    private UUID notificationId;

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
