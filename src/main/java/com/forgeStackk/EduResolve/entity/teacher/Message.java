package com.forgeStackk.EduResolve.entity.teacher;

import com.forgeStackk.EduResolve.enums.MessageContentType;
import com.forgeStackk.EduResolve.enums.RecipientType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "message")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "sender_role", nullable = false, length = 20)
    private String senderRole = "TEACHER";

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 50)
    private RecipientType recipientType;

    @Column(name = "target_class_id")
    private UUID targetClassId;

    @Column(name = "target_subject_id")
    private Long targetSubjectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private MessageContentType contentType;

    @Column(name = "text_body", columnDefinition = "TEXT")
    private String textBody;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "message_id")
    @ToString.Exclude
    private List<MessageAttachment> attachments = new ArrayList<>();

    @Column(name = "sent_at", updatable = false)
    private Instant sentAt;

    @Column(name = "is_homework", nullable = false)
    private Boolean isHomework = false;

    @Column(name = "homework_due_date")
    private LocalDate homeworkDueDate;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "message_id")
    @ToString.Exclude
    private List<ReadReceipt> readReceipts = new ArrayList<>();

    @PrePersist
    void onSend() {
        this.sentAt = Instant.now();
    }
}
