package com.forgeStackk.EduResolve.entity.teacher;

import com.forgeStackk.EduResolve.enums.InboxReadStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "parent_inbox", indexes = {
        @Index(name = "idx_pi_parent_id", columnList = "parent_id"),
        @Index(name = "idx_pi_parent_status", columnList = "parent_id, read_status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentInbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "inbox_id")
    private UUID inboxId;

    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "broadcast_id")
    private Long broadcastId;

    @Enumerated(EnumType.STRING)
    @Column(name = "read_status", nullable = false, length = 10)
    private InboxReadStatus readStatus = InboxReadStatus.UNREAD;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @PrePersist
    void onReceive() {
        this.receivedAt = Instant.now();
    }
}
