package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "attendance_anomalies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceAnomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_id", nullable = false, length = 20)
    private String classId;

    @Column(name = "detected_date", nullable = false)
    private LocalDate detectedDate;

    @Column(name = "class_avg_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal classAvgPct;

    @Column(name = "expected_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal expectedPct;

    @Column(name = "drop_amount", nullable = false, precision = 5, scale = 2)
    private BigDecimal dropAmount;

    @Column(name = "acknowledged_by_user_id")
    private Long acknowledgedByUserId;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }
}
