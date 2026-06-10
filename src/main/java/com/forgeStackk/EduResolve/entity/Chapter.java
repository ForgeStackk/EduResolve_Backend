package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chapter")
@Data
@NoArgsConstructor
public class Chapter {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(columnDefinition = "TEXT")
    private String summary;

    /** Estimated minutes to learn the whole chapter. Powers "Revise in 5 min". */
    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;
}
