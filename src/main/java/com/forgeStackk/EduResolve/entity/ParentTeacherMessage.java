package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "parent_teacher_message")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentTeacherMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_user_id")
    private Long parentUserId;

    @Column(name = "class_name", length = 50)
    private String className;

    @Column(name = "sender_role", length = 20)
    private String senderRole;

    @Column(name = "sender_name", length = 255)
    private String senderName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
