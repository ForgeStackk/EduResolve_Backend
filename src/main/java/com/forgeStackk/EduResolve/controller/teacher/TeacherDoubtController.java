package com.forgeStackk.EduResolve.controller.teacher;

import com.forgeStackk.EduResolve.dto.student.submission.DoubtMessageDto;
import com.forgeStackk.EduResolve.dto.student.submission.DoubtThreadDto;
import com.forgeStackk.EduResolve.security.TeacherPortalAuthHelper;
import com.forgeStackk.EduResolve.service.student.DoubtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher-portal/doubts")
@RequiredArgsConstructor
public class TeacherDoubtController {

    private final DoubtService            doubtService;
    private final TeacherPortalAuthHelper authHelper;

    /** List all doubt threads assigned to this teacher, optionally filtered by studentClass. */
    @GetMapping
    public ResponseEntity<List<DoubtThreadDto>> list(
            @RequestParam(required = false) String studentClass) {
        Long teacherUserId = authHelper.resolveUserLoginId();
        return ResponseEntity.ok(doubtService.listByTeacher(teacherUserId, studentClass));
    }

    /** Get one thread with all messages. */
    @GetMapping("/{threadId}")
    public ResponseEntity<DoubtThreadDto> getThread(@PathVariable Long threadId) {
        return ResponseEntity.ok(doubtService.getThread(threadId));
    }

    /** Teacher replies to a student doubt. */
    @PostMapping(value = "/{threadId}/reply", consumes = "multipart/form-data")
    public ResponseEntity<DoubtMessageDto> reply(
            @PathVariable Long threadId,
            @RequestParam(required = false) String textBody,
            @RequestPart(required = false) MultipartFile voiceNote,
            @RequestPart(required = false) List<MultipartFile> images) {

        Long teacherUserId = authHelper.resolveUserLoginId();
        return ResponseEntity.ok(
                doubtService.reply(threadId, teacherUserId, "TEACHER", textBody, voiceNote, images));
    }

    /** Teacher resolves a doubt thread. */
    @PatchMapping("/{threadId}/resolve")
    public ResponseEntity<Void> resolve(@PathVariable Long threadId) {
        doubtService.resolveByTeacher(threadId, authHelper.resolveUserLoginId());
        return ResponseEntity.noContent().build();
    }
}
