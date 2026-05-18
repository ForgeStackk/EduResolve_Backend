package com.forgeStackk.EduResolve.entity.teacher;

import com.forgeStackk.EduResolve.enums.AttachmentFileType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "message_attachment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "attachment_id")
    private UUID attachmentId;

    // insertable/updatable=false: managed by Message's @JoinColumn
    @Column(name = "message_id", nullable = false, insertable = false, updatable = false)
    private UUID messageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 20)
    private AttachmentFileType fileType;

    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "mime_type", length = 100)
    private String mimeType;
}
