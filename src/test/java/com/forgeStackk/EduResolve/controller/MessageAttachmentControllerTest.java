package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.teacher.MessageAttachment;
import com.forgeStackk.EduResolve.enums.AttachmentFileType;
import com.forgeStackk.EduResolve.repository.teacher.MessageAttachmentRepository;
import com.forgeStackk.EduResolve.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = MessageAttachmentController.class,
            excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@ActiveProfiles("test")
@WithMockUser
class MessageAttachmentControllerTest {

    @TempDir static Path tempDir;

    @Autowired MockMvc mockMvc;

    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean MessageAttachmentRepository attachmentRepo;

    private Path pdfFile;
    private Path audioFile;

    @BeforeEach
    void createFiles() throws Exception {
        pdfFile = tempDir.resolve("test.pdf");
        Files.write(pdfFile, "%PDF-1.4 fake content".getBytes());

        audioFile = tempDir.resolve("voice.webm");
        Files.write(audioFile, "fake-audio".getBytes());
    }

    // ── found, PDF → download disposition ────────────────────────────────────

    @Test
    void getContent_pdfAttachment_returnsFileWithAttachmentDisposition() throws Exception {
        UUID id = UUID.randomUUID();
        MessageAttachment att = attachment(id, pdfFile, AttachmentFileType.DOCUMENT,
                "application/pdf", "homework.pdf");
        when(attachmentRepo.findById(id)).thenReturn(Optional.of(att));

        mockMvc.perform(get("/api/v1/messages/attachments/{id}/content", id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("homework.pdf")))
                .andExpect(content().contentType("application/pdf"));
    }

    // ── voice file → inline disposition ──────────────────────────────────────

    @Test
    void getContent_voiceAttachment_returnsInlineDisposition() throws Exception {
        UUID id = UUID.randomUUID();
        MessageAttachment att = attachment(id, audioFile, AttachmentFileType.VOICE,
                "audio/webm", "voice.webm");
        when(attachmentRepo.findById(id)).thenReturn(Optional.of(att));

        mockMvc.perform(get("/api/v1/messages/attachments/{id}/content", id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("inline")));
    }

    // ── missing DB record → 404 ───────────────────────────────────────────────

    @Test
    void getContent_attachmentNotInDb_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(attachmentRepo.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/messages/attachments/{id}/content", id))
                .andExpect(status().isNotFound());
    }

    // ── file on disk missing → 404 ────────────────────────────────────────────

    @Test
    void getContent_fileDeletedFromDisk_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        Path missing = tempDir.resolve("ghost.pdf");
        MessageAttachment att = attachment(id, missing, AttachmentFileType.DOCUMENT,
                "application/pdf", "ghost.pdf");
        when(attachmentRepo.findById(id)).thenReturn(Optional.of(att));

        mockMvc.perform(get("/api/v1/messages/attachments/{id}/content", id))
                .andExpect(status().isNotFound());
    }

    // ── null mimeType falls back to octet-stream ──────────────────────────────

    @Test
    void getContent_nullMimeType_fallsBackToOctetStream() throws Exception {
        UUID id = UUID.randomUUID();
        MessageAttachment att = attachment(id, pdfFile, AttachmentFileType.DOCUMENT,
                null, "unknown_file");
        when(attachmentRepo.findById(id)).thenReturn(Optional.of(att));

        mockMvc.perform(get("/api/v1/messages/attachments/{id}/content", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/octet-stream"));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private MessageAttachment attachment(UUID id, Path filePath, AttachmentFileType type,
                                         String mimeType, String fileName) {
        MessageAttachment att = new MessageAttachment();
        att.setAttachmentId(id);
        att.setMessageId(UUID.randomUUID());
        att.setFileType(type);
        att.setFileUrl(filePath.toString());
        att.setFileName(fileName);
        att.setFileSizeBytes(100L);
        att.setMimeType(mimeType);
        return att;
    }
}
