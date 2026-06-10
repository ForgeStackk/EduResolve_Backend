package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "admin_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminProfile {

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

    /** principal | vice-principal | coordinator | etc. */
    @Column(length = 100)
    private String designation;

    @Column(length = 100)
    private String department;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    /** full | limited */
    @Column(name = "access_level", length = 30)
    private String accessLevel;

    /** active | inactive */
    @Column(length = 30)
    private String status;
}
