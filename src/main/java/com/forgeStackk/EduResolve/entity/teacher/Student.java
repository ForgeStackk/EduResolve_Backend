package com.forgeStackk.EduResolve.entity.teacher;

import com.forgeStackk.EduResolve.enums.StudentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "tp_student")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "roll_number", nullable = false, length = 50)
    private String rollNumber;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    // insertable/updatable=false: managed by Parent's @JoinColumn
    @Column(name = "parent_id", insertable = false, updatable = false)
    private UUID parentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudentStatus status = StudentStatus.ACTIVE;

    @Column(name = "user_id")
    private Long userId;
}
