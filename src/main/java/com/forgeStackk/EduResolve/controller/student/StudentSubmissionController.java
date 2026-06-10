package com.forgeStackk.EduResolve.controller.student;

import com.forgeStackk.EduResolve.dto.student.submission.HomeworkSubmissionDto;
import com.forgeStackk.EduResolve.security.StudentPortalAuthHelper;
import com.forgeStackk.EduResolve.service.student.HomeworkSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/student-portal/submissions")
@RequiredArgsConstructor
public class StudentSubmissionController {

    private final HomeworkSubmissionService submissionService;
    private final StudentPortalAuthHelper   authHelper;

    /** Submit (or resubmit) homework for an assignment. assignmentId = message.msg_num */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<HomeworkSubmissionDto> submit(
            @RequestParam Long assignmentId,
            @RequestParam(required = false) String textCaption,
            @RequestPart(required = false) MultipartFile voiceNote,
            @RequestPart(required = false) List<MultipartFile> images,
            @RequestPart(required = false) List<MultipartFile> files) {

        Long studentId = authHelper.resolveUserLoginId();
        return ResponseEntity.ok(
                submissionService.submit(studentId, assignmentId, textCaption, voiceNote, images, files));
    }

    /** Get submission status for one assignment. */
    @GetMapping("/status")
    public ResponseEntity<HomeworkSubmissionDto> getStatus(@RequestParam Long assignmentId) {
        Long studentId = authHelper.resolveUserLoginId();
        return ResponseEntity.ok(submissionService.getStatus(studentId, assignmentId));
    }
}
