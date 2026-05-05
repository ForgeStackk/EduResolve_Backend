package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subject")
@Data
@NoArgsConstructor
public class Subject {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 10)
    private String grade; // e.g. "9", "10", "11", "12"

    @Column(length = 50)
    private String icon; // emoji or icon hint

    @Column(length = 50)
    private String colorHex; // tailwind-friendly hex like #667eea
}
