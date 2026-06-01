package com.forgeStackk.EduResolve.classroom.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "classroom_study_groups",
    indexes = @Index(name = "idx_study_groups_classroom", columnList = "classroom_id, owner_user_id"))
@Data
@NoArgsConstructor
public class StudyGroup {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist  void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate   void onUpdate() { updatedAt = Instant.now(); }
}
