package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "notice")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_audience", length = 100)
    private String targetAudience;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    /** Comma-separated channel list e.g. "whatsapp,sms" */
    @Column(length = 100)
    private String channels;

    @Column(name = "sent_at")
    private Instant sentAt;

    @PrePersist
    void onCreate() {
        if (this.sentAt == null) this.sentAt = Instant.now();
    }
}
