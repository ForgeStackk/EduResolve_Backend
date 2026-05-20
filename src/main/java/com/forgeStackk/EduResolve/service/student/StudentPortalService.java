package com.forgeStackk.EduResolve.service.student;

import com.forgeStackk.EduResolve.dto.student.AttendanceDayDto;
import com.forgeStackk.EduResolve.dto.student.StudentInboxItemDto;
import com.forgeStackk.EduResolve.entity.Subject;
import com.forgeStackk.EduResolve.entity.teacher.*;
import com.forgeStackk.EduResolve.enums.InboxReadStatus;
import com.forgeStackk.EduResolve.enums.MessageCategory;
import com.forgeStackk.EduResolve.enums.RecipientType;
import com.forgeStackk.EduResolve.repository.SubjectRepository;
import com.forgeStackk.EduResolve.repository.teacher.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentPortalService {

    private final StudentInboxRepository studentInboxRepo;
    private final MessageRepository messageRepo;
    private final MessageAttachmentRepository attachmentRepo;
    private final AttendanceRepository attendanceRepo;
    private final TeacherRepository teacherRepo;
    private final SubjectRepository subjectRepo;

    public List<StudentInboxItemDto> getInbox(UUID studentId, MessageCategory category, int page, int size) {
        var inboxPage = studentInboxRepo.findByStudentIdOrderByReceivedAtDesc(
                studentId, PageRequest.of(page, size));

        List<UUID> messageIds = inboxPage.stream()
                .map(StudentInbox::getMessageId).toList();
        if (messageIds.isEmpty()) return List.of();

        Map<UUID, Message> messages = messageRepo.findAllById(messageIds)
                .stream().collect(Collectors.toMap(Message::getMessageId, Function.identity()));

        Set<UUID> senderIds = messages.values().stream()
                .map(Message::getSenderId).collect(Collectors.toSet());
        Map<UUID, String> senderNames = teacherRepo.findAllById(senderIds)
                .stream().collect(Collectors.toMap(Teacher::getTeacherId, Teacher::getFullName));

        Set<Long> subjectIds = messages.values().stream()
                .map(Message::getTargetSubjectId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> subjectNames = subjectIds.isEmpty() ? Map.of()
                : subjectRepo.findAllById(subjectIds)
                        .stream().collect(Collectors.toMap(Subject::getId, Subject::getName));

        return inboxPage.stream()
                .map(inbox -> {
                    Message msg = messages.get(inbox.getMessageId());
                    if (msg == null) return null;
                    MessageCategory derived = deriveCategory(msg);
                    if (category != null && derived != category) return null;
                    return toDto(inbox, msg, senderNames, subjectNames);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional
    public void markRead(UUID studentId, UUID inboxId) {
        StudentInbox inbox = studentInboxRepo.findById(inboxId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inbox item not found"));
        if (!inbox.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        inbox.setReadStatus(InboxReadStatus.READ);
        studentInboxRepo.save(inbox);
    }

    public long getUnreadCount(UUID studentId) {
        return studentInboxRepo.countByStudentIdAndReadStatus(studentId, InboxReadStatus.UNREAD);
    }

    public List<AttendanceDayDto> getAttendance(UUID studentId, int month, int year) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        return attendanceRepo.findByStudentIdAndDateBetween(studentId, from, to)
                .stream()
                .map(a -> new AttendanceDayDto(a.getDate(), a.getStatus().name(), a.getRemarks()))
                .sorted(Comparator.comparing(AttendanceDayDto::date))
                .toList();
    }

    private MessageCategory deriveCategory(Message msg) {
        if (Boolean.TRUE.equals(msg.getIsHomework())) return MessageCategory.HOMEWORK;
        if (msg.getRecipientType() == RecipientType.ABSENT_GUARDIANS) return MessageCategory.ABSENCE_NOTIFICATION;
        if (msg.getTargetSubjectId() != null) return MessageCategory.SUBJECT_MESSAGE;
        return MessageCategory.CLASS_NOTICE;
    }

    private StudentInboxItemDto toDto(StudentInbox inbox, Message msg,
                                      Map<UUID, String> senderNames, Map<Long, String> subjectNames) {
        List<MessageAttachment> rawAttachments = attachmentRepo.findByMessageId(msg.getMessageId());
        List<StudentInboxItemDto.AttachmentInfo> attachmentInfos = rawAttachments.stream()
                .map(a -> new StudentInboxItemDto.AttachmentInfo(
                        a.getAttachmentId(),
                        a.getFileType().name(),
                        a.getFileName(),
                        a.getFileSizeBytes() != null ? a.getFileSizeBytes() : 0L,
                        a.getMimeType()))
                .toList();

        return new StudentInboxItemDto(
                inbox.getInboxId(),
                msg.getMessageId(),
                senderNames.getOrDefault(msg.getSenderId(), "Teacher"),
                deriveCategory(msg),
                msg.getTargetSubjectId(),
                msg.getTargetSubjectId() != null ? subjectNames.get(msg.getTargetSubjectId()) : null,
                msg.getTextBody(),
                msg.getContentType().name(),
                msg.getSentAt(),
                Boolean.TRUE.equals(msg.getIsHomework()),
                msg.getHomeworkDueDate(),
                inbox.getReadStatus().name(),
                attachmentInfos);
    }
}
