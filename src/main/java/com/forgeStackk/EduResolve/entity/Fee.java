package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "fee")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Fee {

    public enum Status { Paid, Unpaid }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "student_name", length = 255)
    private String studentName;

    @Column(name = "class_name", length = 50)
    private String className;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private Status status = Status.Unpaid;

    @Column(name = "last_reminder_at")
    private Instant lastReminderAt;
}
