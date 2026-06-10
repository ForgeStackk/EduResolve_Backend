package com.forgeStackk.EduResolve.classroom.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "classroom_messages", indexes = {
    @Index(name = "idx_classroom_msg_room",   columnList = "room_id, created_at DESC"),
    @Index(name = "idx_classroom_msg_sender", columnList = "sender_id, created_at DESC")
})
@Data
@NoArgsConstructor
public class ClassroomMessage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    /** GENERAL | SUBJECT */
    @Column(name = "room_type", nullable = false, length = 20)
    private String roomType;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "sender_role", nullable = false, length = 20)
    private String senderRole;

    /** TEXT | VOICE | FILE | NOTE_SHARE */
    @Column(name = "message_type", nullable = false, length = 20)
    private String messageType;

    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "attachment_type", length = 100)
    private String attachmentType;

    @Column(name = "attachment_name", length = 255)
    private String attachmentName;

    @Column(name = "shared_note_id")
    private Long sharedNoteId;

    @Column(name = "reply_to_message_id")
    private Long replyToMessageId;

    @Column(name = "is_pinned", nullable = false)
    private boolean isPinned = false;

    @Column(name = "pinned_by_user_id")
    private Long pinnedByUserId;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_by_user_id")
    private Long deletedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }
}
