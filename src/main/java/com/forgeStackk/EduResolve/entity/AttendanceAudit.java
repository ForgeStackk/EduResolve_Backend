package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attendance_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attendance_id", nullable = false)
    private UUID attendanceId;

    @Column(name = "changed_by_user_id", nullable = false)
    private Long changedByUserId;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @Column(name = "old_status", nullable = false, length = 20)
    private String oldStatus;

    @Column(name = "new_status", nullable = false, length = 20)
    private String newStatus;

    @Column(name = "old_reason_code", length = 20)
    private String oldReasonCode;

    @Column(name = "new_reason_code", length = 20)
    private String newReasonCode;

    @Column(columnDefinition = "TEXT")
    private String note;

    @PrePersist
    void onCreate() { this.changedAt = Instant.now(); }
}
