package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reading_progress")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadingProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "chapter_id")
    private Long chapterId;

    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "last_read_at", nullable = false)
    private java.time.LocalDateTime lastReadAt;
}
