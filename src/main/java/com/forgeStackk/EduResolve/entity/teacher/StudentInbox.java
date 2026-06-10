package com.forgeStackk.EduResolve.entity.teacher;

import com.forgeStackk.EduResolve.enums.InboxReadStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_inbox", indexes = {
        @Index(name = "idx_si_student_id", columnList = "student_id"),
        @Index(name = "idx_si_student_status", columnList = "student_id, read_status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentInbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "inbox_id")
    private UUID inboxId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

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
