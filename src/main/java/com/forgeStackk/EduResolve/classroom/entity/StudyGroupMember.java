package com.forgeStackk.EduResolve.classroom.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "classroom_study_group_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_id"}),
    indexes = @Index(name = "idx_study_group_members", columnList = "group_id, user_id"))
@Data
@NoArgsConstructor
public class StudyGroupMember {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @PrePersist void onCreate() { joinedAt = Instant.now(); }
}
