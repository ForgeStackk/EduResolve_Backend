package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_document",
       indexes = @Index(name = "idx_student_doc_student", columnList = "student_id"))
@Getter @Setter
public class StudentDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false)
    private String filePath;

    private Long fileSize;

    @Column(length = 50)
    private String contentType;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    void prePersist() {
        uploadedAt = LocalDateTime.now();
    }
}
