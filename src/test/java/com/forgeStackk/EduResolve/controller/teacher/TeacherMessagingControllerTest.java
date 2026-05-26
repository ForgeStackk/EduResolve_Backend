package com.forgeStackk.EduResolve.controller.teacher;

import com.forgeStackk.EduResolve.dto.teacher.MessageSummaryResponse;
import com.forgeStackk.EduResolve.dto.teacher.SendMessageResponse;
import com.forgeStackk.EduResolve.enums.MessageContentType;
import com.forgeStackk.EduResolve.enums.RecipientType;
import com.forgeStackk.EduResolve.security.JwtUtil;
import com.forgeStackk.EduResolve.security.TeacherPortalAuthHelper;
import com.forgeStackk.EduResolve.service.teacher.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = TeacherMessagingController.class,
            excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@ActiveProfiles("test")
@WithMockUser(roles = "TEACHER")
class TeacherMessagingControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean MessageService messageService;
    @MockitoBean TeacherPortalAuthHelper authHelper;

    private final UUID TEACHER_ID = UUID.randomUUID();
    private final UUID CLASS_ID   = UUID.randomUUID();

    @BeforeEach
    void setup() {
        when(authHelper.resolveTeacherId()).thenReturn(TEACHER_ID);
    }

    // ── POST /send ────────────────────────────────────────────────────────────

    @Test
    void send_textOnly_returns200WithDeliveredCount() throws Exception {
        SendMessageResponse resp = new SendMessageResponse(UUID.randomUUID(), Instant.now(), 5);
        when(messageService.send(any(), any(), any(), any(), any(), anyBoolean(),
                any(), any(), any(), any(), any())).thenReturn(resp);

        mockMvc.perform(multipart("/api/v1/teacher-portal/messages/send")
                        .param("targetClassId", CLASS_ID.toString())
                        .param("recipientType", "CLASS")
                        .param("textBody", "Good morning class!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveredTo").value(5));
    }

    @Test
    void send_withVoiceAndFile_returns200() throws Exception {
        SendMessageResponse resp = new SendMessageResponse(UUID.randomUUID(), Instant.now(), 3);
        when(messageService.send(any(), any(), any(), any(), any(), anyBoolean(),
                any(), any(), any(), any(), any())).thenReturn(resp);

        MockMultipartFile voiceFile = new MockMultipartFile(
                "voiceNote", "voice.webm", "audio/webm", "audio".getBytes());
        MockMultipartFile docFile = new MockMultipartFile(
                "files", "notes.pdf", "application/pdf", "pdf".getBytes());

        mockMvc.perform(multipart("/api/v1/teacher-portal/messages/send")
                        .file(voiceFile)
                        .file(docFile)
                        .param("targetClassId", CLASS_ID.toString())
                        .param("recipientType", "CLASS")
                        .param("textBody", "See attachment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveredTo").value(3));
    }

    @Test
    void send_asHomework_returns200() throws Exception {
        SendMessageResponse resp = new SendMessageResponse(UUID.randomUUID(), Instant.now(), 10);
        when(messageService.send(any(), any(), any(), any(), any(), anyBoolean(),
                any(), any(), any(), any(), any())).thenReturn(resp);

        mockMvc.perform(multipart("/api/v1/teacher-portal/messages/send")
                        .param("targetClassId", CLASS_ID.toString())
                        .param("recipientType", "CLASS")
                        .param("textBody", "Complete exercises 1-10")
                        .param("isHomework", "true")
                        .param("homeworkDueDate", "2026-06-01"))
                .andExpect(status().isOk());
    }

    @Test
    void send_serviceThrowsRuntimeException_returns500() throws Exception {
        when(messageService.send(any(), any(), any(), any(), any(), anyBoolean(),
                any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Attachment upload failed: disk full"));

        mockMvc.perform(multipart("/api/v1/teacher-portal/messages/send")
                        .param("targetClassId", CLASS_ID.toString())
                        .param("recipientType", "CLASS")
                        .param("textBody", "Test"))
                .andExpect(status().isInternalServerError());
    }

    // ── GET /sent ─────────────────────────────────────────────────────────────

    @Test
    void getSent_withClassId_returns200PagedResult() throws Exception {
        MessageSummaryResponse summary = new MessageSummaryResponse(
                UUID.randomUUID(), RecipientType.CLASS, CLASS_ID,
                "Hello", MessageContentType.TEXT, Instant.now(),
                false, null, 0, 0L);
        when(messageService.getSent(eq(TEACHER_ID), eq(CLASS_ID), any()))
                .thenReturn(new PageImpl<>(List.of(summary)));

        mockMvc.perform(get("/api/v1/teacher-portal/messages/sent")
                        .param("classId", CLASS_ID.toString())
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].textBody").value("Hello"));
    }

    @Test
    void getSent_noClassId_returnsAllByTeacher() throws Exception {
        when(messageService.getSent(eq(TEACHER_ID), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/teacher-portal/messages/sent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
