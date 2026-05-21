package com.forgeStackk.EduResolve.service.teacher;

import com.forgeStackk.EduResolve.dto.teacher.SendMessageResponse;
import com.forgeStackk.EduResolve.entity.teacher.*;
import com.forgeStackk.EduResolve.enums.*;
import com.forgeStackk.EduResolve.repository.UserLoginRepository;
import com.forgeStackk.EduResolve.repository.teacher.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageServiceTest {

    @TempDir Path tempDir;

    @Mock MessageRepository messageRepo;
    @Mock MessageAttachmentRepository attachmentRepo;
    @Mock ReadReceiptRepository receiptRepo;
    @Mock StudentRepository studentRepo;
    @Mock StudentInboxRepository studentInboxRepo;
    @Mock ParentInboxRepository parentInboxRepo;
    @Mock AttendanceRepository attendanceRepo;
    @Mock ClassRoomRepository classRoomRepo;
    @Mock TeacherRepository teacherRepo;
    @Mock UserLoginRepository userLoginRepo;
    @Mock SimpMessagingTemplate ws;

    @InjectMocks MessageService service;

    private final UUID TEACHER_ID = UUID.randomUUID();
    private final UUID CLASS_ID   = UUID.randomUUID();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "uploadRoot", tempDir.toString());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Message stubSave(MessageContentType type) {
        Message m = new Message();
        m.setMessageId(UUID.randomUUID());
        m.setContentType(type);
        m.setSentAt(Instant.now());
        m.setIsHomework(false);
        m.setTargetClassId(CLASS_ID);
        m.setRecipientType(RecipientType.CLASS);
        when(messageRepo.save(any(Message.class))).thenReturn(m);
        return m;
    }

    private Teacher teacher(String name) {
        Teacher t = new Teacher();
        t.setTeacherId(UUID.randomUUID());
        t.setFullName(name);
        return t;
    }

    private Student student(UUID id) {
        Student s = new Student();
        s.setStudentId(id);
        s.setStatus(StudentStatus.ACTIVE);
        return s;
    }

    private Student studentWithParent(UUID studentId, UUID parentId) {
        Student s = student(studentId);
        ReflectionTestUtils.setField(s, "parentId", parentId);
        return s;
    }

    private MockMultipartFile pdf(String name) {
        return new MockMultipartFile("files", name, "application/pdf",
                ("PDF:" + name).getBytes());
    }

    private MockMultipartFile image(String name) {
        return new MockMultipartFile("images", name, "image/jpeg", new byte[]{1, 2, 3});
    }

    private MockMultipartFile voice() {
        return new MockMultipartFile("voiceNote", "voice.webm", "audio/webm",
                "audio-bytes".getBytes());
    }

    private void stubFanOut(int studentCount) {
        List<Student> students = java.util.stream.IntStream.range(0, studentCount)
                .mapToObj(i -> student(UUID.randomUUID())).toList();
        when(studentRepo.findByClassIdAndStatus(eq(CLASS_ID), eq(StudentStatus.ACTIVE)))
                .thenReturn(students);
        Teacher t = teacher("Ms. Priya");
        when(teacherRepo.findById(TEACHER_ID)).thenReturn(Optional.of(t));
        when(userLoginRepo.findById(any())).thenReturn(Optional.empty());
        when(studentInboxRepo.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── send: text only ───────────────────────────────────────────────────────

    @Test
    void send_textOnly_createsMessageAndFansOutToStudents() {
        stubSave(MessageContentType.TEXT);
        stubFanOut(2);

        SendMessageResponse resp = service.send(TEACHER_ID, CLASS_ID, null,
                RecipientType.CLASS, "Namaste class!", false, null, null, null, null, null);

        assertThat(resp.getDeliveredTo()).isEqualTo(2);
        verify(attachmentRepo, never()).saveAll(any());
        verify(studentInboxRepo).saveAll(argThat(rows -> ((List<?>) rows).size() == 2));
    }

    @Test
    void send_textOnly_noStudentsInClass_delivers0() {
        stubSave(MessageContentType.TEXT);
        when(teacherRepo.findById(TEACHER_ID)).thenReturn(Optional.of(teacher("Mr. Singh")));
        when(studentRepo.findByClassIdAndStatus(eq(CLASS_ID), eq(StudentStatus.ACTIVE)))
                .thenReturn(List.of());

        SendMessageResponse resp = service.send(TEACHER_ID, CLASS_ID, null,
                RecipientType.CLASS, "Hello", false, null, null, null, null, null);

        assertThat(resp.getDeliveredTo()).isZero();
        verify(studentInboxRepo, never()).saveAll(any());
    }

    // ── send: voice note ──────────────────────────────────────────────────────

    @Test
    void send_voiceNote_createsVoiceAttachment() {
        Message saved = stubSave(MessageContentType.VOICE);
        when(teacherRepo.findById(TEACHER_ID)).thenReturn(Optional.of(teacher("Ms. Priya")));
        when(studentRepo.findByClassIdAndStatus(eq(CLASS_ID), eq(StudentStatus.ACTIVE)))
                .thenReturn(List.of());

        service.send(TEACHER_ID, CLASS_ID, null, RecipientType.CLASS, null,
                false, null, null, voice(), null, null);

        verify(attachmentRepo).saveAll(argThat(list -> {
            List<MessageAttachment> atts = (List<MessageAttachment>) list;
            return atts.size() == 1 && atts.get(0).getFileType() == AttachmentFileType.VOICE;
        }));
    }

    // ── send: images ──────────────────────────────────────────────────────────

    @Test
    void send_images_createsImageAttachments() {
        stubSave(MessageContentType.IMAGE);
        when(teacherRepo.findById(TEACHER_ID)).thenReturn(Optional.of(teacher("Ms. Priya")));
        when(studentRepo.findByClassIdAndStatus(eq(CLASS_ID), eq(StudentStatus.ACTIVE)))
                .thenReturn(List.of());

        service.send(TEACHER_ID, CLASS_ID, null, RecipientType.CLASS, null,
                false, null, null, null, List.of(image("photo1.jpg"), image("photo2.jpg")), null);

        verify(attachmentRepo).saveAll(argThat(list -> {
            List<MessageAttachment> atts = (List<MessageAttachment>) list;
            return atts.size() == 2
                    && atts.stream().allMatch(a -> a.getFileType() == AttachmentFileType.IMAGE);
        }));
    }

    // ── send: document files ──────────────────────────────────────────────────

    @Test
    void send_files_createsDocumentAttachments() {
        stubSave(MessageContentType.FILE);
        when(teacherRepo.findById(TEACHER_ID)).thenReturn(Optional.of(teacher("Ms. Priya")));
        when(studentRepo.findByClassIdAndStatus(eq(CLASS_ID), eq(StudentStatus.ACTIVE)))
                .thenReturn(List.of());

        service.send(TEACHER_ID, CLASS_ID, null, RecipientType.CLASS, "See PDFs",
                false, null, null, null, null, List.of(pdf("notes.pdf"), pdf("hw.pdf")));

        verify(attachmentRepo).saveAll(argThat(list -> {
            List<MessageAttachment> atts = (List<MessageAttachment>) list;
            return atts.size() == 2
                    && atts.stream().allMatch(a -> a.getFileType() == AttachmentFileType.DOCUMENT);
        }));
    }

    // ── send: composite (text + voice + image + file = MIXED) ─────────────────

    @Test
    void send_allPayloadTypes_createsMixedWithAllAttachments() {
        stubSave(MessageContentType.MIXED);
        stubFanOut(3);

        SendMessageResponse resp = service.send(TEACHER_ID, CLASS_ID, null, RecipientType.CLASS,
                "See everything attached", false, null, null,
                voice(), List.of(image("snap.jpg")), List.of(pdf("ch1.pdf"), pdf("ch2.pdf")));

        assertThat(resp.getDeliveredTo()).isEqualTo(3);
        verify(attachmentRepo).saveAll(argThat(list -> {
            List<MessageAttachment> atts = (List<MessageAttachment>) list;
            long voices = atts.stream().filter(a -> a.getFileType() == AttachmentFileType.VOICE).count();
            long images = atts.stream().filter(a -> a.getFileType() == AttachmentFileType.IMAGE).count();
            long docs   = atts.stream().filter(a -> a.getFileType() == AttachmentFileType.DOCUMENT).count();
            return atts.size() == 4 && voices == 1 && images == 1 && docs == 2;
        }));
    }

    // ── send: IOException → rollback ─────────────────────────────────────────

    @Test
    void send_ioExceptionOnFirstFile_throwsRuntimeExceptionAndSkipsFanOut() throws Exception {
        stubSave(MessageContentType.FILE);

        MultipartFile badFile = mock(MultipartFile.class);
        when(badFile.isEmpty()).thenReturn(false);
        when(badFile.getOriginalFilename()).thenReturn("broken.pdf");
        when(badFile.getContentType()).thenReturn("application/pdf");
        when(badFile.getSize()).thenReturn(512L);
        doThrow(new IOException("disk full")).when(badFile).transferTo(any(File.class));

        assertThatThrownBy(() ->
                service.send(TEACHER_ID, CLASS_ID, null, RecipientType.CLASS, "See PDF",
                        false, null, null, null, null, List.of(badFile))
        ).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Attachment upload failed");

        verify(attachmentRepo, never()).saveAll(any());
        verify(studentInboxRepo, never()).saveAll(any());
    }

    @Test
    void send_ioExceptionOnVoiceNote_throwsAndSkipsFanOut() throws Exception {
        stubSave(MessageContentType.VOICE);

        MultipartFile badVoice = mock(MultipartFile.class);
        when(badVoice.isEmpty()).thenReturn(false);
        when(badVoice.getOriginalFilename()).thenReturn("voice.webm");
        when(badVoice.getContentType()).thenReturn("audio/webm");
        when(badVoice.getSize()).thenReturn(256L);
        doThrow(new IOException("permission denied")).when(badVoice).transferTo(any(File.class));

        assertThatThrownBy(() ->
                service.send(TEACHER_ID, CLASS_ID, null, RecipientType.CLASS, null,
                        false, null, null, badVoice, null, null)
        ).isInstanceOf(RuntimeException.class);

        verify(attachmentRepo, never()).saveAll(any());
        verify(studentInboxRepo, never()).saveAll(any());
    }

    // ── send: fan-out — parent rows ────────────────────────────────────────────

    @Test
    void send_classRecipient_studentHasParent_createsParentInboxRow() {
        stubSave(MessageContentType.TEXT);
        UUID parentId = UUID.randomUUID();
        when(teacherRepo.findById(TEACHER_ID)).thenReturn(Optional.of(teacher("Ms. Priya")));
        when(studentRepo.findByClassIdAndStatus(eq(CLASS_ID), eq(StudentStatus.ACTIVE)))
                .thenReturn(List.of(studentWithParent(UUID.randomUUID(), parentId)));
        when(studentInboxRepo.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(parentInboxRepo.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        service.send(TEACHER_ID, CLASS_ID, null, RecipientType.CLASS, "Test",
                false, null, null, null, null, null);

        verify(parentInboxRepo).saveAll(argThat(rows -> ((List<?>) rows).size() == 1));
    }

    // ── send: INDIVIDUAL_STUDENT is a no-op fan-out (not yet implemented) ─────

    @Test
    void send_individualStudentRecipient_delivers0() {
        Message saved = stubSave(MessageContentType.TEXT);
        saved.setRecipientType(RecipientType.INDIVIDUAL_STUDENT);
        when(teacherRepo.findById(TEACHER_ID)).thenReturn(Optional.of(teacher("Ms. Priya")));

        SendMessageResponse resp = service.send(TEACHER_ID, CLASS_ID, null,
                RecipientType.INDIVIDUAL_STUDENT, "Hi!", false, null, null, null, null, null);

        assertThat(resp.getDeliveredTo()).isZero();
        verify(studentInboxRepo, never()).saveAll(any());
    }

    // ── send: empty multipart files are skipped ───────────────────────────────

    @Test
    void send_emptyMultipartFiles_areSkipped() {
        stubSave(MessageContentType.TEXT);
        when(teacherRepo.findById(TEACHER_ID)).thenReturn(Optional.of(teacher("Ms. Priya")));
        when(studentRepo.findByClassIdAndStatus(eq(CLASS_ID), eq(StudentStatus.ACTIVE)))
                .thenReturn(List.of());

        MockMultipartFile emptyFile = new MockMultipartFile("files", "", "application/pdf", new byte[0]);

        service.send(TEACHER_ID, CLASS_ID, null, RecipientType.CLASS, "Text only",
                false, null, null, null, null, List.of(emptyFile));

        verify(attachmentRepo, never()).saveAll(any());
    }

    // ── send: teacher not found falls back to "Teacher" ──────────────────────

    @Test
    void send_teacherNotFound_fallsBackToDefaultName() {
        stubSave(MessageContentType.TEXT);
        when(teacherRepo.findById(TEACHER_ID)).thenReturn(Optional.empty());
        when(studentRepo.findByClassIdAndStatus(eq(CLASS_ID), eq(StudentStatus.ACTIVE)))
                .thenReturn(List.of(student(UUID.randomUUID())));
        when(studentInboxRepo.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() ->
                service.send(TEACHER_ID, CLASS_ID, null, RecipientType.CLASS, "Hi",
                        false, null, null, null, null, null)
        ).doesNotThrowAnyException();
    }

    // ── getSent ───────────────────────────────────────────────────────────────

    @Test
    void getSent_byClassId_delegatesToRepository() {
        Message m = stubSave(MessageContentType.TEXT);
        Page<Message> page = new PageImpl<>(List.of(m));
        when(messageRepo.findByTargetClassIdOrderBySentAtDesc(eq(CLASS_ID), any()))
                .thenReturn(page);
        when(receiptRepo.countByMessageId(any())).thenReturn(0L);
        when(attachmentRepo.findByMessageId(any())).thenReturn(List.of());

        var result = service.getSent(TEACHER_ID, CLASS_ID, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        verify(messageRepo).findByTargetClassIdOrderBySentAtDesc(eq(CLASS_ID), any());
    }

    @Test
    void getSent_noClassId_queriesByTeacherId() {
        Message m = stubSave(MessageContentType.TEXT);
        Page<Message> page = new PageImpl<>(List.of(m));
        when(messageRepo.findBySenderIdOrderBySentAtDesc(eq(TEACHER_ID), any()))
                .thenReturn(page);
        when(receiptRepo.countByMessageId(any())).thenReturn(2L);
        when(attachmentRepo.findByMessageId(any())).thenReturn(List.of());

        var result = service.getSent(TEACHER_ID, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getReadCount()).isEqualTo(2L);
    }

    // ── getHomeworkSent ────────────────────────────────────────────────────────

    @Test
    void getHomeworkSent_delegatesToRepository() {
        Message m = stubSave(MessageContentType.FILE);
        m.setIsHomework(true);
        Page<Message> page = new PageImpl<>(List.of(m));
        when(messageRepo.findHomework(any(), any(), any(), any())).thenReturn(page);
        when(receiptRepo.countByMessageId(any())).thenReturn(0L);
        when(attachmentRepo.findByMessageId(any())).thenReturn(List.of());

        var result = service.getHomeworkSent(TEACHER_ID, CLASS_ID, 1L, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIsHomework()).isTrue();
    }
}
