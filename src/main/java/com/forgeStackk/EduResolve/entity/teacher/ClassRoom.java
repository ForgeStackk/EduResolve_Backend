package com.forgeStackk.EduResolve.entity.teacher;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "classroom")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "class_id")
    private UUID classId;

    @Column(name = "class_name", nullable = false, length = 100)
    private String className;

    @Column(nullable = false, length = 10)
    private String section;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Column(name = "class_teacher_id")
    private UUID classTeacherId;
}
