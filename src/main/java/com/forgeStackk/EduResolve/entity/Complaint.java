package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "complaint")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Complaint {

    public enum Status { Pending, InReview, Resolved, Closed }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(length = 100)
    private String category;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Status status = Status.Pending;

    @Column(length = 20)
    private String priority = "medium";

    @Column(name = "raised_by_name", length = 255)
    private String raisedByName;

    @Column(name = "raised_by_role", length = 20)
    private String raisedByRole;

    @Column(name = "sla_due_at")
    private Instant slaDueAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "assignee_name", length = 255)
    private String assigneeName;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
