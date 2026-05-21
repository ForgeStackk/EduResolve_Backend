package com.forgeStackk.EduResolve.entity.student;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "homework_submission")
@Data
@NoArgsConstructor
public class HomeworkSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id", updatable = false, nullable = false)
    private Long submissionId;

    /** References message.msg_num (BIGINT sequence on the message table). */
    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    /** References user_login.id of the student. */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "text_caption", columnDefinition = "TEXT")
    private String textCaption;

    /** SUBMITTED → REVIEWED → GRADED */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "SUBMITTED";

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "graded_at")
    private Instant gradedAt;

    @OneToMany(mappedBy = "submissionId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HomeworkSubmissionAttachment> attachments = new ArrayList<>();

    @PrePersist
    void prePersist() {
        submittedAt = Instant.now();
    }
}
