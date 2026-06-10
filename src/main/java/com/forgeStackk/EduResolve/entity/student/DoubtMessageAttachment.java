package com.forgeStackk.EduResolve.entity.student;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "doubt_message_attachment")
@Data
@NoArgsConstructor
public class DoubtMessageAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id", updatable = false, nullable = false)
    private Long attachmentId;

    @Column(name = "doubt_message_id", nullable = false)
    private Long doubtMessageId;

    @Column(name = "file_type", nullable = false, length = 20)
    private String fileType;

    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "mime_type", length = 100)
    private String mimeType;
}
