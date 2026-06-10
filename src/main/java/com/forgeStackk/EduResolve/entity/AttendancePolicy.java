package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "attendance_policy")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendancePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_name", nullable = false, unique = true, length = 200)
    private String schoolName;

    @Column(name = "min_attendance_pct", nullable = false)
    private int minAttendancePct = 75;

    @Column(name = "at_risk_drop_pct", nullable = false)
    private int atRiskDropPct = 10;

    @Column(name = "auto_notify_parents", nullable = false)
    private boolean autoNotifyParents = false;

    @Column(name = "auto_notify_threshold_pct", nullable = false)
    private int autoNotifyThresholdPct = 75;

    @Column(name = "auto_notify_cooldown_days", nullable = false)
    private int autoNotifyCooldownDays = 7;

    @Column(name = "last_updated_by", length = 255)
    private String lastUpdatedBy;

    @Column(name = "last_updated_at")
    private Instant lastUpdatedAt;
}
