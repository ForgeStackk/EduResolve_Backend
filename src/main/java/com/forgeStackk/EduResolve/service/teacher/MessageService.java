package com.forgeStackk.EduResolve.service.teacher;

import com.forgeStackk.EduResolve.dto.teacher.InboxNotificationPayload;
import com.forgeStackk.EduResolve.dto.teacher.MessageSummaryResponse;
import com.forgeStackk.EduResolve.dto.teacher.SendMessageResponse;
import com.forgeStackk.EduResolve.entity.teacher.*;
import com.forgeStackk.EduResolve.enums.AttachmentFileType;
import com.forgeStackk.EduResolve.enums.AttendanceStatus;
import com.forgeStackk.EduResolve.enums.MessageContentType;
import com.forgeStackk.EduResolve.enums.RecipientType;
import com.forgeStackk.EduResolve.enums.StudentStatus;
import com.forgeStackk.EduResolve.repository.teacher.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private static final String UPLOAD_ROOT = "uploads/teacher-messages";
    private static final int PREVIEW_LENGTH = 100;

    private final MessageRepository messageRepo;
    private final MessageAttachmentRepository attachmentRepo;
    private final ReadReceiptRepository receiptRepo;
    private final StudentRepository studentRepo;
    private final StudentInboxRepository studentInboxRepo;
    private final ParentInboxRepository parentInboxRepo;
    private final AttendanceRepository attendanceRepo;
    private final TeacherRepository teacherRepo;
    private final SimpMessagingTemplate ws;

    // ── Send ──────────────────────────────────────────────────────────────────

    @Transactional
    public SendMessageResponse send(
            UUID teacherId,
            UUID targetClassId,
            Long targetSubjectId,
            RecipientType recipientType,
            String textBody,
            boolean isHomework,
            LocalDate homeworkDueDate,
            MultipartFile voiceNote,
            List<MultipartFile> images,
            List<MultipartFile> files) {

        // Step 1: persist the Message entity
        Message msg = new Message();
        msg.setSenderId(teacherId);
        msg.setTargetClassId(targetClassId);
        msg.setTargetSubjectId(targetSubjectId);
        msg.setRecipientType(recipientType);
        msg.setTextBody(textBody);
        msg.setIsHomework(isHomework);
        msg.setHomeworkDueDate(homeworkDueDate);
        msg.setContentType(resolveContentType(textBody, voiceNote, images, files));
        Message saved = messageRepo.save(msg);

        // Step 2: persist attachments
        List<MessageAttachment> attachments = new ArrayList<>();
        Path dir = Paths.get(UPLOAD_ROOT, saved.getMessageId().toString());
        try {
            if (hasFile(voiceNote)) {
                attachments.add(saveFile(dir, saved.getMessageId(), voiceNote, AttachmentFileType.VOICE));
            }
            if (images != null) {
                for (MultipartFile img : images) {
                    if (hasFile(img)) attachments.add(saveFile(dir, saved.getMessageId(), img, AttachmentFileType.IMAGE));
                }
            }
            if (files != null) {
                for (MultipartFile f : files) {
                    if (hasFile(f)) attachments.add(saveFile(dir, saved.getMessageId(), f, AttachmentFileType.DOCUMENT));
                }
            }
        } catch (IOException e) {
            log.error("Failed to save attachment for message {}: {}", saved.getMessageId(), e.getMessage());
        }
        if (!attachments.isEmpty()) {
            attachmentRepo.saveAll(attachments);
        }

        // Steps 3-5: fan-out → inbox rows + WebSocket push
        String senderName = teacherRepo.findById(teacherId)
                .map(Teacher::getFullName)
                .orElse("Teacher");
        int deliveredTo = fanOut(saved, senderName);

        return new SendMessageResponse(saved.getMessageId(), saved.getSentAt(), deliveredTo);
    }

    // ── Paginated sent messages ───────────────────────────────────────────────

    public Page<MessageSummaryResponse> getSent(UUID teacherId, UUID classId, Pageable pageable) {
        Page<Message> page = (classId != null)
                ? messageRepo.findByTargetClassIdOrderBySentAtDesc(classId, pageable)
                : messageRepo.findBySenderIdOrderBySentAtDesc(teacherId, pageable);
        return page.map(this::toSummary);
    }

    // ── Paginated sent homework ───────────────────────────────────────────────

    public Page<MessageSummaryResponse> getHomeworkSent(UUID teacherId, UUID classId, Long subjectId, Pageable pageable) {
        return messageRepo.findHomework(teacherId, classId, subjectId, pageable).map(this::toSummary);
    }

    // ── Fan-out ───────────────────────────────────────────────────────────────

    /**
     * Resolves recipients, inserts StudentInbox / ParentInbox rows, and fires
     * a STOMP message to /topic/inbox/{recipientId} for each recipient.
     *
     * @return total number of students reached
     */
    private int fanOut(Message msg, String senderName) {
        List<Student> students = resolveRecipients(msg);
        if (students.isEmpty()) {
            return 0;
        }

        InboxNotificationPayload payload = new InboxNotificationPayload(
                msg.getMessageId(),
                buildPreview(msg.getTextBody()),
                senderName,
                msg.getSentAt());

        List<StudentInbox> studentRows = new ArrayList<>(students.size());
        List<ParentInbox> parentRows = new ArrayList<>(students.size());

        for (Student student : students) {
            // 3a. Student inbox row
            StudentInbox si = new StudentInbox();
            si.setStudentId(student.getStudentId());
            si.setMessageId(msg.getMessageId());
            studentRows.add(si);

            // 3b. WebSocket push to student
            pushNotification(student.getStudentId(), payload);

            // 3c-3d. Parent inbox row + push
            if (student.getParentId() != null) {
                ParentInbox pi = new ParentInbox();
                pi.setParentId(student.getParentId());
                pi.setMessageId(msg.getMessageId());
                parentRows.add(pi);

                pushNotification(student.getParentId(), payload);
            }
        }

        studentInboxRepo.saveAll(studentRows);
        if (!parentRows.isEmpty()) {
            parentInboxRepo.saveAll(parentRows);
        }

        return students.size();
    }

    // ── Recipient resolution ──────────────────────────────────────────────────

    private List<Student> resolveRecipients(Message msg) {
        return switch (msg.getRecipientType()) {
            case CLASS -> studentRepo.findByClassIdAndStatus(msg.getTargetClassId(), StudentStatus.ACTIVE);
            case ABSENT_GUARDIANS -> resolveAbsentStudents(msg.getTargetClassId());
            // INDIVIDUAL_STUDENT / INDIVIDUAL_PARENT handled at a higher level;
            // fan-out for those is a no-op here (direct delivery via specific student/parent id).
            default -> List.of();
        };
    }

    private List<Student> resolveAbsentStudents(UUID classId) {
        Set<UUID> absentIds = attendanceRepo
                .findByClassIdAndDate(classId, LocalDate.now())
                .stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT)
                .map(Attendance::getStudentId)
                .collect(Collectors.toSet());

        return studentRepo.findByClassIdAndStatus(classId, StudentStatus.ACTIVE)
                .stream()
                .filter(s -> absentIds.contains(s.getStudentId()))
                .toList();
    }

    // ── WebSocket push ────────────────────────────────────────────────────────

    private void pushNotification(UUID recipientId, InboxNotificationPayload payload) {
        try {
            ws.convertAndSend("/topic/inbox/" + recipientId, payload);
        } catch (Exception e) {
            log.warn("WebSocket push failed for recipientId={}: {}", recipientId, e.getMessage());
        }
    }

    // ── File storage ──────────────────────────────────────────────────────────

    private MessageAttachment saveFile(Path dir, UUID messageId, MultipartFile file, AttachmentFileType type)
            throws IOException {
        Files.createDirectories(dir);
        String safeName = System.currentTimeMillis() + "_" +
                (file.getOriginalFilename() != null
                        ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                        : "file");
        Path dest = dir.resolve(safeName);
        file.transferTo(dest.toFile());

        MessageAttachment att = new MessageAttachment();
        att.setMessageId(messageId);
        att.setFileType(type);
        att.setFileUrl(dest.toString());
        att.setFileName(file.getOriginalFilename());
        att.setFileSizeBytes(file.getSize());
        att.setMimeType(file.getContentType());
        return att;
    }

    private boolean hasFile(MultipartFile f) {
        return f != null && !f.isEmpty();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private MessageContentType resolveContentType(String text, MultipartFile voice,
                                                   List<MultipartFile> images, List<MultipartFile> files) {
        boolean hasText = text != null && !text.isBlank();
        boolean hasVoice = hasFile(voice);
        boolean hasImage = images != null && images.stream().anyMatch(this::hasFile);
        boolean hasDoc = files != null && files.stream().anyMatch(this::hasFile);

        int kinds = (hasText ? 1 : 0) + (hasVoice ? 1 : 0) + (hasImage ? 1 : 0) + (hasDoc ? 1 : 0);
        if (kinds > 1) return MessageContentType.MIXED;
        if (hasVoice) return MessageContentType.VOICE;
        if (hasImage) return MessageContentType.IMAGE;
        if (hasDoc) return MessageContentType.FILE;
        return MessageContentType.TEXT;
    }

    private String buildPreview(String textBody) {
        if (textBody == null || textBody.isBlank()) return "(attachment)";
        return textBody.length() > PREVIEW_LENGTH
                ? textBody.substring(0, PREVIEW_LENGTH) + "…"
                : textBody;
    }

    private MessageSummaryResponse toSummary(Message m) {
        long reads = receiptRepo.countByMessageId(m.getMessageId());
        int attCount = attachmentRepo.findByMessageId(m.getMessageId()).size();
        return new MessageSummaryResponse(
                m.getMessageId(), m.getRecipientType(), m.getTargetClassId(),
                m.getTextBody(), m.getContentType(), m.getSentAt(),
                m.getIsHomework(), m.getHomeworkDueDate(), attCount, reads);
    }
}
