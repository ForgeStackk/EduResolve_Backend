package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "complaint_reply")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "complaint_id", nullable = false)
    private Long complaintId;

    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "author_name", length = 255)
    private String authorName;

    @Column(name = "author_role", length = 20)
    private String authorRole;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }
}
