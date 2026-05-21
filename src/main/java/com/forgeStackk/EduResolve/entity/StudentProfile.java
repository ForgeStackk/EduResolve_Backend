package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "student_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 10)
    private String initials;

    @Column(length = 20)
    private String color;

    @Column(name = "engagement")
    private Integer engagement;

    @Column(length = 10)
    private String grade;

    /** excellent | good | at-risk */
    @Column(length = 30)
    private String status;

    @Column(name = "class_name", length = 50)
    private String className;

    @Column(name = "streak_days")
    private Integer streakDays;

    @Column(name = "experience_points")
    private Integer experiencePoints;

    @Column(name = "top_percentage")
    private Integer topPercentage;

    @Column(name = "login_days")
    private Integer loginDays;

    /** Last calendar day (IST) on which a login was recorded — used for idempotent increment. */
    @Column(name = "last_login_date")
    private LocalDate lastLoginDate;
}
