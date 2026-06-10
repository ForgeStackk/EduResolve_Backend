package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "scheduled_job_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledJobLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Column(name = "ran_at", nullable = false, updatable = false)
    private Instant ranAt;

    @Column(name = "students_scanned", nullable = false)
    private int studentsScanned = 0;

    @Column(name = "at_risk_found", nullable = false)
    private int atRiskFound = 0;

    @Column(name = "notifications_sent", nullable = false)
    private int notificationsSent = 0;

    @Column(columnDefinition = "TEXT")
    private String errors;

    @PrePersist
    void onCreate() { this.ranAt = Instant.now(); }
}
