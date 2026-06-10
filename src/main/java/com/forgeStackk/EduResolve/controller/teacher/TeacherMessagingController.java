package com.forgeStackk.EduResolve.controller.teacher;

import com.forgeStackk.EduResolve.dto.teacher.MessageSummaryResponse;
import com.forgeStackk.EduResolve.dto.teacher.SendMessageResponse;
import com.forgeStackk.EduResolve.enums.RecipientType;
import com.forgeStackk.EduResolve.security.TeacherPortalAuthHelper;
import com.forgeStackk.EduResolve.service.teacher.MessageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher-portal/messages")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class TeacherMessagingController {

    private final MessageService messageService;
    private final TeacherPortalAuthHelper authHelper;

    // ── JSON body helper ─────────────────────────────────────────────────────
    /** Simple DTO for JSON-based class notice (non-multipart path). */
    @Data
    public static class ClassNoticeRequest {
        @NotNull(message = "targetClassId is required")
        private UUID targetClassId;
        private Long targetSubjectId;
        @NotBlank(message = "textBody is required")
        private String textBody;
        private RecipientType recipientType = RecipientType.CLASS;
    }

    /**
     * POST /messages  (application/json)
     * Allows teachers to send a text-only class notice without multipart overhead.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SendMessageResponse> sendJson(
            @Valid @RequestBody ClassNoticeRequest req) {
        UUID teacherId = authHelper.resolveTeacherId();
        SendMessageResponse response = messageService.send(
                teacherId, req.getTargetClassId(), req.getTargetSubjectId(),
                req.getRecipientType(), req.getTextBody(),
                false, null, null, null, null, null);
        return ResponseEntity.ok(response);
    }

    // POST /messages/send  (multipart/form-data)
    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SendMessageResponse> send(
            @RequestParam("targetClassId") UUID targetClassId,
            @RequestParam(value = "targetSubjectId", required = false) Long targetSubjectId,
            @RequestParam("recipientType") RecipientType recipientType,
            @RequestParam(value = "textBody", required = false) String textBody,
            @RequestParam(value = "isHomework", defaultValue = "false") boolean isHomework,
            @RequestParam(value = "homeworkDueDate", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate homeworkDueDate,
            @RequestParam(value = "attendanceDate", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate attendanceDate,
            @RequestPart(value = "voiceNote", required = false) MultipartFile voiceNote,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        UUID teacherId = authHelper.resolveTeacherId();
        SendMessageResponse response = messageService.send(
                teacherId, targetClassId, targetSubjectId, recipientType,
                textBody, isHomework, homeworkDueDate, attendanceDate, voiceNote, images, files);
        return ResponseEntity.ok(response);
    }

    // GET /messages/sent?classId=&page=&size=
    @GetMapping("/sent")
    public ResponseEntity<Page<MessageSummaryResponse>> sent(
            @RequestParam(required = false) UUID classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID teacherId = authHelper.resolveTeacherId();
        Page<MessageSummaryResponse> result = messageService.getSent(teacherId, classId,
                PageRequest.of(page, size));
        return ResponseEntity.ok(result);
    }
}
