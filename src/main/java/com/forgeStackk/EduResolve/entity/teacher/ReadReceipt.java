package com.forgeStackk.EduResolve.entity.teacher;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "read_receipt")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "receipt_id")
    private UUID receiptId;

    // insertable/updatable=false: managed by Message's @JoinColumn
    @Column(name = "message_id", nullable = false, insertable = false, updatable = false)
    private UUID messageId;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "read_at", updatable = false)
    private Instant readAt;

    @PrePersist
    void onRead() {
        this.readAt = Instant.now();
    }
}
