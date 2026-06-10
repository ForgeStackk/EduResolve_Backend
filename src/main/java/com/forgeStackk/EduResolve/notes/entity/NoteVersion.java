package com.forgeStackk.EduResolve.notes.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "note_versions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"note_id", "version_number"}),
    indexes = @Index(name = "idx_note_versions_note", columnList = "note_id, version_number DESC"))
@Data
@NoArgsConstructor
public class NoteVersion {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "note_id", nullable = false)
    private Long noteId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "content_snapshot", nullable = false, columnDefinition = "TEXT")
    private String contentSnapshot;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(name = "edited_at", nullable = false, updatable = false)
    private Instant editedAt;

    @PrePersist void onCreate() { editedAt = Instant.now(); }
}
