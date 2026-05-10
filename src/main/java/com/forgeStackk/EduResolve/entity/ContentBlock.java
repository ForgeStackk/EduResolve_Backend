package com.forgeStackk.EduResolve.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Content Block entity representing structured content extracted from NCERT PDFs.
 * Can be text or image, linked to a chapter.
 */
@Entity
@Table(name = "content_block", indexes = {
    @Index(name = "idx_content_block_chapter", columnList = "chapter_id"),
    @Index(name = "idx_content_block_type", columnList = "block_type")
})
@Data
@NoArgsConstructor
public class ContentBlock {

    public enum BlockType { TEXT, IMAGE, DIAGRAM, TABLE, HEADING }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 20)
    private BlockType blockType;

    @Column(columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl; // GCS URL for extracted images

    @Column(name = "image_filename", length = 255)
    private String imageFilename;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(length = 255)
    private String heading; // For heading-type blocks

    @Column(name = "page_number")
    private Integer pageNumber; // Source page in PDF
}
