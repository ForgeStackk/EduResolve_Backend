package com.forgeStackk.EduResolve.notes.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "note_revision_log",
    indexes = @Index(name = "idx_revision_log_note", columnList = "note_id, revised_at DESC"))
@Data
@NoArgsConstructor
public class NoteRevisionLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "note_id", nullable = false)
    private Long noteId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "revised_at", nullable = false, updatable = false)
    private Instant revisedAt;

    /** GOT_IT or REVIEW_AGAIN */
    @Column(nullable = false, length = 20)
    private String result;

    @PrePersist void onCreate() { revisedAt = Instant.now(); }
}
