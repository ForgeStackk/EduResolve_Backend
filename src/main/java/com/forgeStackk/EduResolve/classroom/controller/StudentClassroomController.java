package com.forgeStackk.EduResolve.classroom.controller;

import com.forgeStackk.EduResolve.classroom.dto.*;
import com.forgeStackk.EduResolve.classroom.service.StudentClassroomService;
import com.forgeStackk.EduResolve.entity.UserLogin;
import com.forgeStackk.EduResolve.repository.UserLoginRepository;
import com.forgeStackk.EduResolve.security.StudentPortalAuthHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/student/classroom")
@RequiredArgsConstructor
public class StudentClassroomController {

    private final StudentClassroomService classroomService;
    private final StudentPortalAuthHelper auth;
    private final UserLoginRepository     userLoginRepo;

    // ── S. Get / provision classroom ──────────────────────────────────────────

    @GetMapping
    public ClassroomResponse getClassroom() {
        Long userLoginId = auth.resolveUserLoginId();
        UserLogin user   = userLoginRepo.findById(userLoginId)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                HttpStatus.FORBIDDEN, "User not found"));
        String classLabel = user.getClassName();
        String schoolName = user.getSchoolName();
        if (classLabel == null || classLabel.isBlank())
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.BAD_REQUEST, "User has no class assigned");
        return classroomService.getOrProvision(userLoginId, classLabel, schoolName);
    }

    // ── T. Subject rooms ─────────────────────────────────────────────────────

    @GetMapping("/{classroomId}/subject-rooms")
    public List<SubjectRoomResponse> listSubjectRooms(@PathVariable Long classroomId) {
        Long userLoginId = auth.resolveUserLoginId();
        String schoolName = resolveSchool(userLoginId);
        return classroomService.listSubjectRooms(classroomId, userLoginId, schoolName);
    }

    // ── U. Send message ──────────────────────────────────────────────────────

    @PostMapping("/{classroomId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassroomMessageDto sendMessage(
            @PathVariable Long classroomId,
            @RequestParam(required = false) Long subjectRoomId,
            @RequestBody SendMessageRequest req) {
        Long userLoginId = auth.resolveUserLoginId();
        String schoolName = resolveSchool(userLoginId);
        return classroomService.sendMessage(classroomId, userLoginId, schoolName, subjectRoomId, req);
    }

    // ── V. Message history ───────────────────────────────────────────────────

    @GetMapping("/{classroomId}/messages")
    public List<ClassroomMessageDto> getMessages(
            @PathVariable Long classroomId,
            @RequestParam(required = false) Long subjectRoomId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "30") int limit) {
        Long userLoginId = auth.resolveUserLoginId();
        String schoolName = resolveSchool(userLoginId);
        return classroomService.getMessages(classroomId, userLoginId, schoolName,
            subjectRoomId, beforeId, limit);
    }

    // ── W. Delete message ────────────────────────────────────────────────────

    @DeleteMapping("/{classroomId}/messages/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(
            @PathVariable Long classroomId,
            @PathVariable Long messageId,
            @RequestParam(required = false) Long subjectRoomId) {
        Long userLoginId = auth.resolveUserLoginId();
        String schoolName = resolveSchool(userLoginId);
        classroomService.deleteMessage(classroomId, messageId, userLoginId, subjectRoomId, schoolName);
    }

    // ── X. React ─────────────────────────────────────────────────────────────

    @PostMapping("/{classroomId}/messages/{messageId}/react")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void react(
            @PathVariable Long classroomId,
            @PathVariable Long messageId,
            @RequestParam(required = false) Long subjectRoomId,
            @RequestBody ReactRequest req) {
        Long userLoginId = auth.resolveUserLoginId();
        String schoolName = resolveSchool(userLoginId);
        classroomService.react(classroomId, messageId, userLoginId, subjectRoomId, schoolName, req);
    }

    // ── Y. Pin / unpin ───────────────────────────────────────────────────────

    @PostMapping("/{classroomId}/messages/{messageId}/pin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void pinMessage(
            @PathVariable Long classroomId,
            @PathVariable Long messageId,
            @RequestParam(required = false) Long subjectRoomId) {
        Long userLoginId = auth.resolveUserLoginId();
        String schoolName = resolveSchool(userLoginId);
        classroomService.pinMessage(classroomId, messageId, userLoginId, subjectRoomId, schoolName);
    }

    @DeleteMapping("/{classroomId}/messages/{messageId}/pin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unpinMessage(
            @PathVariable Long classroomId,
            @PathVariable Long messageId,
            @RequestParam(required = false) Long subjectRoomId) {
        Long userLoginId = auth.resolveUserLoginId();
        String schoolName = resolveSchool(userLoginId);
        classroomService.unpinMessage(classroomId, messageId, userLoginId, subjectRoomId, schoolName);
    }

    @GetMapping("/{classroomId}/pinned")
    public List<ClassroomMessageDto> getPinned(
            @PathVariable Long classroomId,
            @RequestParam(required = false) Long subjectRoomId) {
        Long userLoginId = auth.resolveUserLoginId();
        String schoolName = resolveSchool(userLoginId);
        return classroomService.getPinned(classroomId, userLoginId, subjectRoomId, schoolName);
    }

    // ── Z. Search ────────────────────────────────────────────────────────────

    @GetMapping("/{classroomId}/search")
    public Page<ClassroomMessageDto> search(
            @PathVariable Long classroomId,
            @RequestParam(required = false) Long subjectRoomId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userLoginId = auth.resolveUserLoginId();
        String schoolName = resolveSchool(userLoginId);
        return classroomService.search(classroomId, userLoginId, subjectRoomId, schoolName, query, page, size);
    }

    // ── AA. Members ──────────────────────────────────────────────────────────

    @GetMapping("/{classroomId}/members")
    public List<ClassroomMemberResponse> listMembers(@PathVariable Long classroomId) {
        Long userLoginId = auth.resolveUserLoginId();
        String schoolName = resolveSchool(userLoginId);
        return classroomService.listMembers(classroomId, userLoginId, schoolName);
    }

    // ── AB. Presence ─────────────────────────────────────────────────────────

    @PostMapping("/{classroomId}/presence")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePresence(
            @PathVariable Long classroomId,
            @RequestParam boolean online) {
        Long userLoginId = auth.resolveUserLoginId();
        String schoolName = resolveSchool(userLoginId);
        classroomService.updatePresence(classroomId, userLoginId, schoolName, online);
    }

    // ── AC. Report ───────────────────────────────────────────────────────────

    @PostMapping("/{classroomId}/messages/{messageId}/report")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reportMessage(
            @PathVariable Long classroomId,
            @PathVariable Long messageId,
            @RequestBody ReportRequest req) {
        Long userLoginId = auth.resolveUserLoginId();
        String schoolName = resolveSchool(userLoginId);
        classroomService.reportMessage(classroomId, messageId, userLoginId, schoolName, req);
    }

    // ── AD. Study groups ──────────────────────────────────────────────────────

    @PostMapping("/{classroomId}/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public StudyGroupResponse createGroup(
            @PathVariable Long classroomId,
            @RequestBody CreateGroupRequest req) {
        Long userLoginId = auth.resolveUserLoginId();
        String schoolName = resolveSchool(userLoginId);
        return classroomService.createGroup(classroomId, userLoginId, schoolName, req);
    }

    @GetMapping("/{classroomId}/groups")
    public List<StudyGroupResponse> listGroups(@PathVariable Long classroomId) {
        Long userLoginId = auth.resolveUserLoginId();
        String schoolName = resolveSchool(userLoginId);
        return classroomService.listMyGroups(classroomId, userLoginId, schoolName);
    }

    @PutMapping("/{classroomId}/groups/{groupId}")
    public StudyGroupResponse updateGroup(
            @PathVariable Long classroomId,
            @PathVariable Long groupId,
            @RequestBody UpdateGroupRequest req) {
        Long userLoginId = auth.resolveUserLoginId();
        String schoolName = resolveSchool(userLoginId);
        return classroomService.updateGroup(classroomId, groupId, userLoginId, schoolName, req);
    }

    @PostMapping("/{classroomId}/groups/{groupId}/members")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addGroupMembers(
            @PathVariable Long classroomId,
            @PathVariable Long groupId,
            @RequestBody List<Long> memberUserIds) {
        Long userLoginId = auth.resolveUserLoginId();
        String schoolName = resolveSchool(userLoginId);
        classroomService.addGroupMembers(classroomId, groupId, userLoginId, schoolName, memberUserIds);
    }

    @DeleteMapping("/{classroomId}/groups/{groupId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeGroupMember(
            @PathVariable Long classroomId,
            @PathVariable Long groupId,
            @PathVariable Long userId) {
        Long userLoginId = auth.resolveUserLoginId();
        String schoolName = resolveSchool(userLoginId);
        classroomService.removeGroupMember(classroomId, groupId, userLoginId, schoolName, userId);
    }

    @DeleteMapping("/{classroomId}/groups/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(
            @PathVariable Long classroomId,
            @PathVariable Long groupId) {
        Long userLoginId = auth.resolveUserLoginId();
        String schoolName = resolveSchool(userLoginId);
        classroomService.deleteGroup(classroomId, groupId, userLoginId, schoolName);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String resolveSchool(Long userLoginId) {
        return userLoginRepo.findById(userLoginId)
            .map(UserLogin::getSchoolName)
            .orElse(null);
    }
}
