package com.forgeStackk.EduResolve.classroom.service;

import com.forgeStackk.EduResolve.classroom.dto.*;
import com.forgeStackk.EduResolve.classroom.entity.*;
import com.forgeStackk.EduResolve.classroom.repository.*;
import com.forgeStackk.EduResolve.entity.UserLogin;
import com.forgeStackk.EduResolve.notes.entity.StudentNote;
import com.forgeStackk.EduResolve.notes.repository.StudentNoteRepository;
import com.forgeStackk.EduResolve.repository.UserLoginRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentClassroomService {

    private final StudentClassroomRepository     classroomRepo;
    private final ClassroomSubjectRoomRepository subjectRoomRepo;
    private final ClassroomMessageRepository     messageRepo;
    private final ClassroomMessageReactionRepository reactionRepo;
    private final ClassroomMemberRepository      memberRepo;
    private final ClassroomPinnedMessageRepository pinnedRepo;
    private final ClassroomReportedMessageRepository reportedRepo;
    private final StudentNoteRepository          noteRepo;
    private final UserLoginRepository            userLoginRepo;
    private final ClassroomWebSocketService      wsService;

    @Value("${classroom.max-pinned-per-room:5}")
    private int maxPinnedPerRoom;

    @Value("${classroom.message-rate-limit-per-minute:30}")
    private int messageRateLimit;

    // ── S. Get or provision classroom ─────────────────────────────────────────

    @Transactional
    public ClassroomResponse getOrProvision(Long userLoginId, String classLabel, String schoolName) {
        StudentClassroom classroom = classroomRepo.findByClassLabelAndSchoolName(classLabel, schoolName)
            .orElseGet(() -> provision(classLabel, schoolName));

        ensureMember(classroom.getId(), userLoginId, "STUDENT");

        return toClassroomResponse(classroom);
    }

    private StudentClassroom provision(String classLabel, String schoolName) {
        StudentClassroom c = new StudentClassroom();
        c.setClassLabel(classLabel);
        c.setSchoolName(schoolName);
        c.setName("Class " + classLabel + " Classroom");
        return classroomRepo.save(c);
    }

    private void ensureMember(Long classroomId, Long userId, String role) {
        if (!memberRepo.existsByClassroomIdAndUserId(classroomId, userId)) {
            ClassroomMember m = new ClassroomMember();
            m.setClassroomId(classroomId);
            m.setUserId(userId);
            m.setRole(role);
            memberRepo.save(m);
        }
    }

    // ── T. Subject rooms ─────────────────────────────────────────────────────

    public List<SubjectRoomResponse> listSubjectRooms(Long classroomId, Long userLoginId, String schoolName) {
        validateMembership(classroomId, userLoginId, schoolName);
        return subjectRoomRepo.findByClassroomIdAndIsActiveTrueOrderByNameAsc(classroomId)
            .stream().map(r -> new SubjectRoomResponse(r.getId(), r.getClassroomId(),
                r.getSubjectId(), null, r.getName(), r.getCreatedAt()))
            .toList();
    }

    // ── U. Send message ──────────────────────────────────────────────────────

    @Transactional
    public ClassroomMessageDto sendMessage(Long classroomId, Long userLoginId, String schoolName,
                                           Long subjectRoomId, SendMessageRequest req) {
        validateMembership(classroomId, userLoginId, schoolName);

        Long roomId   = resolveRoomId(classroomId, subjectRoomId);
        String roomType = subjectRoomId != null ? "SUBJECT" : "GENERAL";

        ClassroomMessage msg = new ClassroomMessage();
        msg.setRoomId(roomId);
        msg.setRoomType(roomType);
        msg.setSenderId(userLoginId);
        msg.setSenderRole("STUDENT");
        msg.setMessageType(req.messageType() != null ? req.messageType() : "TEXT");
        msg.setTextContent(req.textContent());
        msg.setAttachmentUrl(req.attachmentUrl());
        msg.setAttachmentType(req.attachmentType());
        msg.setAttachmentName(req.attachmentName());
        msg.setSharedNoteId(req.sharedNoteId());
        msg.setReplyToMessageId(req.replyToMessageId());

        ClassroomMessage saved = messageRepo.save(msg);
        ClassroomMessageDto dto = toDto(saved, userLoginId);

        wsService.broadcast(classroomId, "NEW_MESSAGE", dto);
        return dto;
    }

    // ── V. Message history (cursor pagination) ───────────────────────────────

    public List<ClassroomMessageDto> getMessages(Long classroomId, Long userLoginId,
                                                  String schoolName, Long subjectRoomId,
                                                  Long beforeId, int limit) {
        validateMembership(classroomId, userLoginId, schoolName);
        Long roomId = resolveRoomId(classroomId, subjectRoomId);
        PageRequest pr = PageRequest.of(0, limit);
        return messageRepo.findByRoomCursor(roomId, beforeId, pr)
            .stream().map(m -> toDto(m, userLoginId)).toList();
    }

    // ── W. Delete message ────────────────────────────────────────────────────

    @Transactional
    public void deleteMessage(Long classroomId, Long messageId, Long userLoginId,
                               Long subjectRoomId, String schoolName) {
        validateMembership(classroomId, userLoginId, schoolName);
        Long roomId = resolveRoomId(classroomId, subjectRoomId);
        ClassroomMessage msg = messageRepo.findByIdAndRoomId(messageId, roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!msg.getSenderId().equals(userLoginId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete another user's message");

        msg.setDeleted(true);
        msg.setDeletedByUserId(userLoginId);
        messageRepo.save(msg);

        wsService.broadcast(classroomId, "MESSAGE_DELETED", Map.of("messageId", messageId));
    }

    // ── X. React to message ──────────────────────────────────────────────────

    @Transactional
    public void react(Long classroomId, Long messageId, Long userLoginId,
                      Long subjectRoomId, String schoolName, ReactRequest req) {
        validateMembership(classroomId, userLoginId, schoolName);
        Long roomId = resolveRoomId(classroomId, subjectRoomId);
        messageRepo.findByIdAndRoomId(messageId, roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        var existing = reactionRepo.findByMessageIdAndUserIdAndEmoji(messageId, userLoginId, req.emoji());
        if (existing.isPresent()) {
            reactionRepo.delete(existing.get());
        } else {
            ClassroomMessageReaction reaction = new ClassroomMessageReaction();
            reaction.setMessageId(messageId);
            reaction.setUserId(userLoginId);
            reaction.setEmoji(req.emoji());
            reactionRepo.save(reaction);
        }

        Map<String, Long> updated = buildReactionCounts(messageId);
        wsService.broadcast(classroomId, "REACTION_UPDATED", Map.of(
            "messageId", messageId, "reactions", updated));
    }

    // ── Y. Pin message ───────────────────────────────────────────────────────

    @Transactional
    public void pinMessage(Long classroomId, Long messageId, Long userLoginId,
                           Long subjectRoomId, String schoolName) {
        validateMembership(classroomId, userLoginId, schoolName);
        Long roomId = resolveRoomId(classroomId, subjectRoomId);
        ClassroomMessage msg = messageRepo.findByIdAndRoomId(messageId, roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        long pinCount = pinnedRepo.countByRoomId(roomId);
        if (pinCount >= maxPinnedPerRoom)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Pin limit of " + maxPinnedPerRoom + " reached for this room");

        ClassroomPinnedMessage pin = new ClassroomPinnedMessage();
        pin.setRoomId(roomId);
        pin.setMessageId(messageId);
        pin.setPinnedByUserId(userLoginId);
        pinnedRepo.save(pin);

        msg.setPinned(true);
        msg.setPinnedByUserId(userLoginId);
        messageRepo.save(msg);

        wsService.broadcast(classroomId, "MESSAGE_PINNED", Map.of("messageId", messageId));
    }

    @Transactional
    public void unpinMessage(Long classroomId, Long messageId, Long userLoginId,
                              Long subjectRoomId, String schoolName) {
        validateMembership(classroomId, userLoginId, schoolName);
        Long roomId = resolveRoomId(classroomId, subjectRoomId);
        ClassroomMessage msg = messageRepo.findByIdAndRoomId(messageId, roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        pinnedRepo.deleteByRoomIdAndMessageId(roomId, messageId);
        msg.setPinned(false);
        msg.setPinnedByUserId(null);
        messageRepo.save(msg);
    }

    public List<ClassroomMessageDto> getPinned(Long classroomId, Long userLoginId,
                                                Long subjectRoomId, String schoolName) {
        validateMembership(classroomId, userLoginId, schoolName);
        Long roomId = resolveRoomId(classroomId, subjectRoomId);
        return pinnedRepo.findByRoomIdOrderByPinnedAtDesc(roomId).stream()
            .map(pin -> messageRepo.findById(pin.getMessageId())
                .map(m -> toDto(m, userLoginId)).orElse(null))
            .filter(d -> d != null)
            .toList();
    }

    // ── Z. Search ────────────────────────────────────────────────────────────

    public Page<ClassroomMessageDto> search(Long classroomId, Long userLoginId,
                                             Long subjectRoomId, String schoolName,
                                             String query, int page, int size) {
        validateMembership(classroomId, userLoginId, schoolName);
        Long roomId = resolveRoomId(classroomId, subjectRoomId);
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return messageRepo.searchInRoom(roomId, query, pr).map(m -> toDto(m, userLoginId));
    }

    // ── AA. Members ──────────────────────────────────────────────────────────

    public List<ClassroomMemberResponse> listMembers(Long classroomId, Long userLoginId, String schoolName) {
        validateMembership(classroomId, userLoginId, schoolName);
        return memberRepo.findByClassroomIdOrderByRoleAscJoinedAtAsc(classroomId).stream()
            .map(m -> {
                String name = userLoginRepo.findById(m.getUserId())
                    .map(u -> u.getFirstName() + " " + u.getLastName()).orElse("Unknown");
                return new ClassroomMemberResponse(m.getUserId(), name, m.getRole(),
                    m.isOnline(), m.getLastSeenAt(), m.getJoinedAt());
            }).toList();
    }

    // ── AB. Presence ─────────────────────────────────────────────────────────

    @Transactional
    public void updatePresence(Long classroomId, Long userLoginId, String schoolName, boolean online) {
        if (!memberRepo.existsByClassroomIdAndUserId(classroomId, userLoginId)) return;
        memberRepo.updatePresence(classroomId, userLoginId, online, Instant.now());
        String event = online ? "MEMBER_ONLINE" : "MEMBER_OFFLINE";
        wsService.broadcast(classroomId, event, Map.of("userId", userLoginId));
    }

    // ── AC. Report message ───────────────────────────────────────────────────

    @Transactional
    public void reportMessage(Long classroomId, Long messageId, Long userLoginId,
                               String schoolName, ReportRequest req) {
        validateMembership(classroomId, userLoginId, schoolName);
        ClassroomReportedMessage report = new ClassroomReportedMessage();
        report.setMessageId(messageId);
        report.setReportedByUserId(userLoginId);
        report.setReason(req.reason());
        reportedRepo.save(report);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void validateMembership(Long classroomId, Long userLoginId, String schoolName) {
        classroomRepo.findByIdAndSchoolName(classroomId, schoolName)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Classroom not found or cross-school access denied"));
        if (!memberRepo.existsByClassroomIdAndUserId(classroomId, userLoginId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this classroom");
    }

    private Long resolveRoomId(Long classroomId, Long subjectRoomId) {
        if (subjectRoomId == null) return classroomId;
        return subjectRoomRepo.findByIdAndClassroomId(subjectRoomId, classroomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject room not found"))
            .getId();
    }

    private Map<String, Long> buildReactionCounts(Long messageId) {
        Map<String, Long> counts = new HashMap<>();
        reactionRepo.countByMessageIdGroupedByEmoji(messageId)
            .forEach(row -> counts.put((String) row[0], (Long) row[1]));
        return counts;
    }

    private ClassroomMessageDto toDto(ClassroomMessage msg, Long currentUserId) {
        Map<String, Long> reactions = buildReactionCounts(msg.getId());
        List<String> myReactions = reactionRepo.findByUserIdAndMessageIdIn(currentUserId,
                List.of(msg.getId())).stream()
            .map(ClassroomMessageReaction::getEmoji).toList();

        String senderName = userLoginRepo.findById(msg.getSenderId())
            .map(u -> u.getFirstName() + " " + u.getLastName()).orElse("Unknown");

        ClassroomMessageDto.NoteSharePreview notePreview = null;
        if ("NOTE_SHARE".equals(msg.getMessageType()) && msg.getSharedNoteId() != null) {
            notePreview = noteRepo.findById(msg.getSharedNoteId())
                .map(n -> new ClassroomMessageDto.NoteSharePreview(
                    n.getId(), n.getTitle(), n.getLanguage(), null,
                    n.getContent() != null && n.getContent().length() > 150
                        ? n.getContent().substring(0, 150) : n.getContent()))
                .orElse(null);
        }

        String textContent = msg.isDeleted() ? null : msg.getTextContent();

        return new ClassroomMessageDto(msg.getId(), msg.getRoomId(), msg.getRoomType(),
            msg.getSenderId(), senderName, msg.getSenderRole(), msg.getMessageType(),
            textContent, msg.getAttachmentUrl(), msg.getAttachmentType(), msg.getAttachmentName(),
            notePreview, msg.getReplyToMessageId(), msg.isPinned(), msg.isDeleted(),
            reactions, myReactions, msg.getCreatedAt());
    }

    private ClassroomResponse toClassroomResponse(StudentClassroom c) {
        long members = memberRepo.countByClassroomId(c.getId());
        long online  = memberRepo.countByClassroomIdAndIsOnlineTrue(c.getId());
        return new ClassroomResponse(c.getId(), c.getName(), c.getClassLabel(), c.getSchoolName(), members, online);
    }
}
