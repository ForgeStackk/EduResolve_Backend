package com.forgeStackk.EduResolve.entity.teacher;

import com.forgeStackk.EduResolve.enums.AttendanceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
    name = "attendance",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_attendance_class_student_date",
        columnNames = {"class_id", "student_id", "date"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "attendance_id")
    private UUID attendanceId;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(name = "marked_by", nullable = false)
    private UUID markedBy;

    @Column(name = "marked_at", updatable = false)
    private Instant markedAt;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Version
    private Long version;

    @PrePersist
    void onMark() {
        this.markedAt = Instant.now();
    }
}
