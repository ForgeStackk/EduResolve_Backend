package com.forgeStackk.EduResolve.entity.teacher;

import com.forgeStackk.EduResolve.enums.TeacherStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "teacher")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "teacher_id")
    private UUID teacherId;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "class_teacher_of")
    private UUID classTeacherOf;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "teacher_id")
    @ToString.Exclude
    private List<TeacherSubjectMapping> assignedSubjects = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeacherStatus status = TeacherStatus.ACTIVE;
}
