package com.forgeStackk.EduResolve.controller.student;

import com.forgeStackk.EduResolve.dto.student.AttendanceDayDto;
import com.forgeStackk.EduResolve.dto.student.StudentInboxItemDto;
import com.forgeStackk.EduResolve.enums.MessageCategory;
import com.forgeStackk.EduResolve.security.JwtUtil;
import com.forgeStackk.EduResolve.security.StudentPortalAuthHelper;
import com.forgeStackk.EduResolve.service.student.StudentPortalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentInboxController.class)
class StudentInboxControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean StudentPortalService studentPortalService;
    @MockitoBean StudentPortalAuthHelper authHelper;

    private final UUID STUDENT_ID = UUID.randomUUID();

    @BeforeEach
    void setup() {
        when(authHelper.resolveStudentId()).thenReturn(STUDENT_ID);
    }

    // ── GET /inbox ────────────────────────────────────────────────────────────

    @Test
    void getInbox_returnsListOfItems() throws Exception {
        StudentInboxItemDto item = new StudentInboxItemDto(
                UUID.randomUUID(), UUID.randomUUID(), null, "Ms. Priya",
                MessageCategory.CLASS_NOTICE, null, null,
                "Welcome back!", "TEXT",
                Instant.now(), false, null, "UNREAD", List.of());

        when(studentPortalService.getInbox(eq(STUDENT_ID), isNull(), eq(0), eq(50)))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/student-portal/inbox")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].textBody").value("Welcome back!"))
                .andExpect(jsonPath("$[0].senderName").value("Ms. Priya"))
                .andExpect(jsonPath("$[0].readStatus").value("UNREAD"));
    }

    @Test
    void getInbox_filteredByCategory_passesCategory() throws Exception {
        when(studentPortalService.getInbox(eq(STUDENT_ID), eq(MessageCategory.HOMEWORK), eq(0), eq(50)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/student-portal/inbox")
                        .param("category", "HOMEWORK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(studentPortalService).getInbox(eq(STUDENT_ID), eq(MessageCategory.HOMEWORK), anyInt(), anyInt());
    }

    @Test
    void getInbox_emptyList_returns200WithEmptyArray() throws Exception {
        when(studentPortalService.getInbox(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/student-portal/inbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── PATCH /inbox/{id}/read ────────────────────────────────────────────────

    @Test
    void markRead_returns204() throws Exception {
        UUID inboxId = UUID.randomUUID();
        doNothing().when(studentPortalService).markRead(STUDENT_ID, inboxId);

        mockMvc.perform(patch("/api/v1/student-portal/inbox/{id}/read", inboxId))
                .andExpect(status().isNoContent());

        verify(studentPortalService).markRead(STUDENT_ID, inboxId);
    }

    // ── GET /inbox/unread-count ───────────────────────────────────────────────

    @Test
    void getUnreadCount_returnsCountInBody() throws Exception {
        when(studentPortalService.getUnreadCount(STUDENT_ID)).thenReturn(7L);

        mockMvc.perform(get("/api/v1/student-portal/inbox/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(7));
    }

    // ── GET /attendance ───────────────────────────────────────────────────────

    @Test
    void getAttendance_returnsListOfDays() throws Exception {
        AttendanceDayDto day = new AttendanceDayDto(LocalDate.of(2025, 5, 1), "PRESENT", null);
        when(studentPortalService.getAttendance(eq(STUDENT_ID), anyInt(), anyInt()))
                .thenReturn(List.of(day));

        mockMvc.perform(get("/api/v1/student-portal/attendance")
                        .param("month", "5")
                        .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PRESENT"));
    }
}
