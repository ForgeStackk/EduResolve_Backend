package com.forgeStackk.EduResolve.entity.teacher;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "teacher_subject_mapping")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSubjectMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    // insertable/updatable=false: the FK is managed by Teacher's @JoinColumn
    @Column(name = "teacher_id", nullable = false, insertable = false, updatable = false)
    private UUID teacherId;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;
}
