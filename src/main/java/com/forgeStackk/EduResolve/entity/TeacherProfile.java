package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "teacher_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherProfile {

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

    @Column(name = "employee_id", length = 50)
    private String employeeId;

    @Column(length = 100)
    private String subject;

    @Column(length = 100)
    private String department;

    @Column(length = 255)
    private String qualification;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "class_name", length = 50)
    private String className;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    /** active | inactive | on-leave */
    @Column(length = 30)
    private String status;
}
