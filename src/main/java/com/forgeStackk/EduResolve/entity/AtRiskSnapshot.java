package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "at_risk_snapshots",
    uniqueConstraints = @UniqueConstraint(name = "uq_at_risk_student_date",
        columnNames = {"student_seq_id", "snapshot_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtRiskSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_seq_id", nullable = false)
    private Long studentSeqId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "attendance_pct", precision = 5, scale = 2)
    private BigDecimal attendancePct;

    @Column(name = "previous_pct", precision = 5, scale = 2)
    private BigDecimal previousPct;

    @Column(name = "reason_dominant", length = 20)
    private String reasonDominant;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }
}
