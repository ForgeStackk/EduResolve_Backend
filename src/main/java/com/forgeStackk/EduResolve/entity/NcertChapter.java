package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NCERT Chapter entity representing a chapter within an NCERT Book.
 * Contains metadata and links to content blocks.
 */
@Entity
@Table(name = "ncert_chapter", indexes = {
    @Index(name = "idx_ncert_chapter_book", columnList = "book_id")
})
@Data
@NoArgsConstructor
public class NcertChapter {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "chapter_number")
    private Integer chapterNumber;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "start_page")
    private Integer startPage;

    @Column(name = "end_page")
    private Integer endPage;
}
