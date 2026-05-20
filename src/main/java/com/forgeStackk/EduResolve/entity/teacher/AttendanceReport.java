package com.forgeStackk.EduResolve.entity.teacher;

import com.forgeStackk.EduResolve.enums.ReportGeneratedBy;
import com.forgeStackk.EduResolve.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attendance_report")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "report_id")
    private UUID reportId;

    @Column(name = "class_id", nullable = false, length = 20)
    private String classId;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "generated_at", updatable = false)
    private Instant generatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "generated_by", nullable = false, length = 20)
    private ReportGeneratedBy generatedBy;

    @Column(name = "report_file_url", columnDefinition = "TEXT")
    private String reportFileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportStatus status = ReportStatus.GENERATED;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @PrePersist
    void onGenerate() {
        this.generatedAt = Instant.now();
    }
}
