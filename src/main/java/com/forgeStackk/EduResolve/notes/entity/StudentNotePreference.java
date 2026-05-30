package com.forgeStackk.EduResolve.notes.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "student_note_preferences")
@Data
@NoArgsConstructor
public class StudentNotePreference {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, unique = true)
    private Long studentId;

    /** 'en' or 'hi' */
    @Column(name = "preferred_language", nullable = false, length = 10)
    private String preferredLanguage = "en";

    /** BRIEF | STANDARD | DETAILED */
    @Column(name = "preferred_note_length", length = 20)
    private String preferredNoteLength = "STANDARD";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist  void onCreate() { updatedAt = Instant.now(); }
    @PreUpdate   void onUpdate() { updatedAt = Instant.now(); }
}
