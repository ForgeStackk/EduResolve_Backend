package com.forgeStackk.EduResolve.entity.student;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "doubt_message")
@Data
@NoArgsConstructor
public class DoubtMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doubt_message_id", updatable = false, nullable = false)
    private Long doubtMessageId;

    @Column(name = "thread_id", nullable = false)
    private Long threadId;

    /** References user_login.id of the sender. */
    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "sender_role", nullable = false, length = 20)
    private String senderRole;

    @Column(name = "text_body", columnDefinition = "TEXT")
    private String textBody;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    @OneToMany(mappedBy = "doubtMessageId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DoubtMessageAttachment> attachments = new ArrayList<>();

    @PrePersist
    void prePersist() {
        sentAt = Instant.now();
    }
}
