package com.forgeStackk.EduResolve.service.student;

import com.forgeStackk.EduResolve.dto.student.submission.DoubtMessageDto;
import com.forgeStackk.EduResolve.dto.student.submission.DoubtThreadDto;
import com.forgeStackk.EduResolve.dto.student.submission.SubmissionAttachmentDto;
import com.forgeStackk.EduResolve.entity.student.*;
import com.forgeStackk.EduResolve.entity.teacher.Teacher;
import com.forgeStackk.EduResolve.entity.teacher.TeacherSubjectMapping;
import com.forgeStackk.EduResolve.entity.UserLogin;
import com.forgeStackk.EduResolve.repository.SubjectRepository;
import com.forgeStackk.EduResolve.repository.UserLoginRepository;
import com.forgeStackk.EduResolve.repository.student.*;
import com.forgeStackk.EduResolve.repository.teacher.ClassRoomRepository;
import com.forgeStackk.EduResolve.repository.teacher.StudentRepository;
import com.forgeStackk.EduResolve.repository.teacher.TeacherRepository;
import com.forgeStackk.EduResolve.repository.teacher.TeacherSubjectMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoubtService {

    @Value("${app.uploads.root:uploads/teacher-messages}")
    private String uploadRoot;

    private final DoubtThreadRepository      threadRepo;
    private final DoubtMessageRepository     messageRepo;
    private final DoubtMessageAttachmentRepository attRepo;
    private final TeacherSubjectMappingRepository  subjectMappingRepo;
    private final TeacherRepository          teacherRepo;
    private final SubjectRepository          subjectRepo;
    private final UserLoginRepository        userLoginRepo;
    private final ClassRoomRepository        classRoomRepo;
    private final StudentRepository          studentRepo;
    private final SimpMessagingTemplate      ws;

    /**
     * Opens a doubt thread from a student to a teacher.
     *
     * @param teacherUserIdOverride  When the student explicitly picks a teacher from the picker,
     *                               pass their user_login.id here; pass null to auto-resolve via
     *                               subject mapping (legacy flow).
     */
    @Transactional
    public DoubtThreadDto openThread(
            Long studentId,
            UUID classId,
            Long teacherUserIdOverride,
            Long subjectId,
            Long chapterId,
            String textBody,
            MultipartFile voiceNote,
            List<MultipartFile> images) {

        // Resolve student's classroom
        UUID resolvedClassId = (classId != null) ? classId :
                studentRepo.findByUserId(studentId)
                        .map(s -> s.getClassId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Student class not found — please complete your profile"));

        assertSameSchool(studentId, resolvedClassId);

        Long teacherUserId = (teacherUserIdOverride != null)
                ? teacherUserIdOverride
                : resolveTeacher(resolvedClassId, subjectId);

        // ── Dedup: reuse existing OPEN thread between same student + teacher ──
        Optional<DoubtThread> existing = threadRepo
                .findFirstByStudentIdAndTeacherIdOrderByCreatedAtDesc(studentId, teacherUserId);
        if (existing.isPresent() && "OPEN".equals(existing.get().getStatus())) {
            DoubtThread existingThread = existing.get();
            DoubtMessage msg = saveMessage(existingThread.getThreadId(), studentId, "STUDENT",
                    textBody, voiceNote, images);
            notifyRecipient(teacherUserId, existingThread.getThreadId(), textBody);
            return getThread(existingThread.getThreadId());
        }

        // ── Resolve denormalized student info for the new thread ──
        String[] studentInfo = resolveStudentInfo(studentId, resolvedClassId);

        DoubtThread thread = new DoubtThread();
        thread.setStudentId(studentId);
        thread.setTeacherId(teacherUserId);
        thread.setSubjectId(subjectId);
        thread.setChapterId(chapterId);
        thread.setStudentName(studentInfo[0]);
        thread.setStudentClass(studentInfo[1]);
        thread.setStudentSection(studentInfo[2]);
        DoubtThread saved = threadRepo.save(thread);

        DoubtMessage msg = saveMessage(saved.getThreadId(), studentId, "STUDENT",
                textBody, voiceNote, images);

        notifyRecipient(teacherUserId, saved.getThreadId(), textBody);

        String teacherName = teacherRepo.findByUserId(teacherUserId)
                .map(Teacher::getFullName).orElse("Teacher");
        String subjectName = subjectId != null
                ? subjectRepo.findById(subjectId).map(s -> s.getName()).orElse(null)
                : null;

        List<DoubtMessageDto> msgs = List.of(toMsgDto(msg,
                attRepo.findByDoubtMessageId(msg.getDoubtMessageId())));
        return new DoubtThreadDto(saved.getThreadId(), studentId, teacherUserId, teacherName,
                subjectId, subjectName, chapterId, saved.getStatus(),
                saved.getCreatedAt(), saved.getResolvedAt(), msgs,
                studentInfo[0], studentInfo[1], studentInfo[2]);
    }

    /** Returns [studentName, studentClass, studentSection]. */
    private String[] resolveStudentInfo(Long studentUserId, UUID classId) {
        String name = studentRepo.findByUserId(studentUserId)
                .map(s -> s.getFullName()).orElse("");
        String className = "";
        String section = "";
        if (classId != null) {
            var room = classRoomRepo.findById(classId);
            if (room.isPresent()) {
                className = room.get().getClassName() != null ? room.get().getClassName() : "";
                section   = room.get().getSection()   != null ? room.get().getSection()   : "";
            }
        }
        return new String[]{ name, className, section };
    }

    @Transactional
    public DoubtMessageDto reply(
            Long threadId,
            Long senderId,
            String senderRole,
            String textBody,
            MultipartFile voiceNote,
            List<MultipartFile> images) {

        DoubtThread thread = threadRepo.findById(threadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found"));

        DoubtMessage msg = saveMessage(threadId, senderId, senderRole, textBody, voiceNote, images);

        Long notifyId = "STUDENT".equals(senderRole) ? thread.getTeacherId() : thread.getStudentId();
        notifyRecipient(notifyId, threadId, textBody);

        return toMsgDto(msg, attRepo.findByDoubtMessageId(msg.getDoubtMessageId()));
    }

    @Transactional
    public void resolve(Long threadId, Long studentId) {
        DoubtThread thread = threadRepo.findById(threadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!thread.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        thread.setStatus("RESOLVED");
        thread.setResolvedAt(Instant.now());
        threadRepo.save(thread);
    }

    @Transactional
    public void resolveByTeacher(Long threadId, Long teacherUserId) {
        DoubtThread thread = threadRepo.findById(threadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!thread.getTeacherId().equals(teacherUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        thread.setStatus("RESOLVED");
        thread.setResolvedAt(Instant.now());
        threadRepo.save(thread);
    }

    public DoubtThreadDto getThread(Long threadId) {
        DoubtThread thread = threadRepo.findById(threadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<DoubtMessage> msgs = messageRepo.findByThreadIdOrderBySentAtAsc(threadId);
        List<DoubtMessageDto> msgDtos = msgs.stream()
                .map(m -> toMsgDto(m, attRepo.findByDoubtMessageId(m.getDoubtMessageId())))
                .toList();

        String teacherName = teacherRepo.findByUserId(thread.getTeacherId())
                .map(Teacher::getFullName).orElse("Teacher");
        String subjectName = thread.getSubjectId() != null
                ? subjectRepo.findById(thread.getSubjectId()).map(s -> s.getName()).orElse(null)
                : null;

        return new DoubtThreadDto(thread.getThreadId(), thread.getStudentId(),
                thread.getTeacherId(), teacherName, thread.getSubjectId(), subjectName,
                thread.getChapterId(), thread.getStatus(), thread.getCreatedAt(),
                thread.getResolvedAt(), msgDtos,
                thread.getStudentName(), thread.getStudentClass(), thread.getStudentSection());
    }

    public List<DoubtThreadDto> listByStudent(Long studentId) {
        return threadRepo.findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(t -> getThread(t.getThreadId()))
                .toList();
    }

    /** Teacher-side: list threads assigned to this teacher, optionally filtered by studentClass. */
    public List<DoubtThreadDto> listByTeacher(Long teacherUserId, String studentClass) {
        List<DoubtThread> threads = (studentClass != null && !studentClass.isBlank())
                ? threadRepo.findByTeacherIdAndStudentClassOrderByCreatedAtDesc(teacherUserId, studentClass)
                : threadRepo.findByTeacherIdOrderByCreatedAtDesc(teacherUserId);
        return threads.stream().map(t -> getThread(t.getThreadId())).toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Resolves the subject teacher and returns their user_login.id (Long). */
    private Long resolveTeacher(UUID classId, Long subjectId) {
        if (subjectId != null && classId != null) {
            UUID teacherUuid = subjectMappingRepo
                    .findByClassIdAndSubjectId(classId, subjectId)
                    .stream().findFirst()
                    .map(TeacherSubjectMapping::getTeacherId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "No teacher found for that subject and class"));
            return teacherRepo.findById(teacherUuid)
                    .map(Teacher::getUserId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Teacher has no linked user account"));
        }
        return teacherRepo.findByClassTeacherOf(classId)
                .map(Teacher::getUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "No class teacher found for this class"));
    }

    private DoubtMessage saveMessage(Long threadId, Long senderId, String senderRole,
                                      String textBody, MultipartFile voiceNote,
                                      List<MultipartFile> images) {
        DoubtMessage msg = new DoubtMessage();
        msg.setThreadId(threadId);
        msg.setSenderId(senderId);
        msg.setSenderRole(senderRole);
        msg.setTextBody(textBody);
        DoubtMessage saved = messageRepo.save(msg);

        Path dir = Paths.get(uploadRoot, "doubts", saved.getDoubtMessageId().toString());
        List<DoubtMessageAttachment> atts = new ArrayList<>();
        try {
            if (hasFile(voiceNote)) atts.add(saveFile(dir, saved.getDoubtMessageId(), voiceNote, "VOICE"));
            if (images != null) {
                for (MultipartFile img : images) {
                    if (hasFile(img)) atts.add(saveFile(dir, saved.getDoubtMessageId(), img, "IMAGE"));
                }
            }
            if (!atts.isEmpty()) attRepo.saveAll(atts);
        } catch (IOException e) {
            throw new RuntimeException("Attachment upload failed: " + e.getMessage(), e);
        }
        return saved;
    }

    private DoubtMessageAttachment saveFile(Path dir, Long msgId,
                                             MultipartFile file, String type) throws IOException {
        Files.createDirectories(dir);
        String safeName = System.currentTimeMillis() + "_" +
                (file.getOriginalFilename() != null
                        ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                        : "file");
        Path dest = dir.resolve(safeName).toAbsolutePath();
        file.transferTo(dest);

        DoubtMessageAttachment att = new DoubtMessageAttachment();
        att.setDoubtMessageId(msgId);
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

    private void notifyRecipient(Long recipientId, Long threadId, String text) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "DOUBT");
            payload.put("threadId", threadId);
            payload.put("preview", text != null && text.length() > 80 ? text.substring(0, 80) + "…" : text);
            ws.convertAndSend("/topic/inbox/" + recipientId, (Object) payload);
        } catch (Exception e) {
            log.warn("WebSocket push failed for doubt notification: {}", e.getMessage());
        }
    }

    // ── School isolation ──────────────────────────────────────────────────────

    private void assertSameSchool(Long studentUserId, UUID classId) {
        if (classId == null) return;

        String studentSchool = userLoginRepo.findById(studentUserId)
                .map(UserLogin::getSchoolName)
                .orElse(null);

        String classSchool = classRoomRepo.findById(classId)
                .map(cr -> cr.getSchoolName())
                .orElse(null);

        if (studentSchool != null && classSchool != null
                && !studentSchool.equalsIgnoreCase(classSchool)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Cannot send a doubt to a teacher from a different school");
        }
    }

    private DoubtMessageDto toMsgDto(DoubtMessage m, List<DoubtMessageAttachment> atts) {
        List<SubmissionAttachmentDto> attDtos = atts.stream()
                .map(a -> new SubmissionAttachmentDto(
                        a.getAttachmentId(), a.getFileType(),
                        a.getFileName(), a.getFileSizeBytes() != null ? a.getFileSizeBytes() : 0L,
                        a.getMimeType()))
                .toList();
        return new DoubtMessageDto(m.getDoubtMessageId(), m.getThreadId(),
                m.getSenderId(), m.getSenderRole(), m.getTextBody(), m.getSentAt(), attDtos);
    }
}
