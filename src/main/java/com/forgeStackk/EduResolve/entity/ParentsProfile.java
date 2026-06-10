package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "parents_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentsProfile {

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

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "student_name", length = 255)
    private String studentName;

    @Column(name = "class_name", length = 50)
    private String className;

    /** father | mother | guardian */
    @Column(length = 30)
    private String relation;

    @Column(length = 100)
    private String occupation;

    /** active | inactive */
    @Column(length = 30)
    private String status;
}
