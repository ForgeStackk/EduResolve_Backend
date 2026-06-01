package com.forgeStackk.EduResolve.notes.service;

import com.forgeStackk.EduResolve.notes.dto.*;
import com.forgeStackk.EduResolve.notes.entity.NoteVersion;
import com.forgeStackk.EduResolve.notes.entity.StudentNote;
import com.forgeStackk.EduResolve.notes.entity.StudentNotePreference;
import com.forgeStackk.EduResolve.notes.repository.*;
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
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentNotesService {

    private final StudentNoteRepository           noteRepo;
    private final NoteVersionRepository           versionRepo;
    private final StudentNotePreferenceRepository prefRepo;
    private final PdfExtractionJobRepository      pdfJobRepo;
    private final NotesAiService                  aiService;

    @Value("${notes.daily-note-limit:20}")
    private int dailyNoteLimit;

    @Value("${notes.max-note-versions:10}")
    private int maxNoteVersions;

    @Value("${notes.system-prompt.en}")
    private String systemPromptEn;

    @Value("${notes.system-prompt.hi}")
    private String systemPromptHi;

    // ── A. Generate (SSE stream) ──────────────────────────────────────────────

    public Flux<String> generateStream(Long studentId, String schoolName, GenerateNoteRequest req) {
        enforceRateLimit(studentId);

        String lang         = req.language() != null ? req.language() : "en";
        String systemPrompt = "hi".equals(lang) ? systemPromptHi : systemPromptEn;
        String lengthHint   = resolveLengthHint(studentId, req.noteLength());
        String fullSystem   = systemPrompt + "\n\nNote length preference: " + lengthHint;
        String userContent  = buildUserContent(req, studentId);

        AtomicReference<StringBuilder> buffer = new AtomicReference<>(new StringBuilder());

        return aiService.generateNoteStream(fullSystem, userContent)
            .doOnNext(delta -> buffer.get().append(delta))
            .doOnComplete(() -> persistGeneratedNote(studentId, schoolName, req, lang, buffer.get().toString()))
            .doOnError(e -> log.error("Note stream error for student={}: {}", studentId, e.getMessage()));
    }

    private String buildUserContent(GenerateNoteRequest req, Long studentId) {
        String sourceType = req.sourceType() != null ? req.sourceType() : "TOPIC_INPUT";
        return switch (sourceType) {
            case "PDF_UPLOAD" -> buildPdfContent(req.pdfJobId(), studentId);
            case "PHOTO_OCR"  -> "Generate notes from this image content:\n" + req.photoUrl();
            case "VOICE"      -> "Generate notes from this voice transcript:\n" + req.voiceFileUrl();
            default           -> req.prompt() != null ? req.prompt() : "Generate comprehensive notes.";
        };
    }

    private String buildPdfContent(Long pdfJobId, Long studentId) {
        if (pdfJobId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pdfJobId required for PDF_UPLOAD");
        var job = pdfJobRepo.findById(pdfJobId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF job not found"));
        if (!job.getStudentId().equals(studentId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "PDF job does not belong to this student");
        if (!"COMPLETED".equals(job.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "PDF extraction not yet complete: " + job.getStatus());
        return "Generate structured notes from this PDF content:\n\n" + job.getExtractedText();
    }

    private String resolveLengthHint(Long studentId, String requestLength) {
        if (requestLength != null) return requestLength;
        return prefRepo.findByStudentId(studentId)
            .map(StudentNotePreference::getPreferredNoteLength)
            .orElse("STANDARD");
    }

    void persistGeneratedNote(Long studentId, String schoolName,
                              GenerateNoteRequest req, String lang, String content) {
        if (content == null || content.isBlank()) return;

        StudentNote note = new StudentNote();
        note.setStudentId(studentId);
        note.setSchoolName(schoolName);
        note.setTitle(extractTitle(content, req));
        note.setContent(content);
        note.setRawPrompt(req.prompt());
        note.setSourceType(req.sourceType() != null ? req.sourceType() : "TOPIC_INPUT");
        note.setLanguage(lang);
        note.setSubjectId(req.subjectId());
        note.setAiModelUsed("gpt-4o-mini");

        if (req.pdfJobId() != null) {
            pdfJobRepo.findById(req.pdfJobId()).ifPresent(job -> {
                note.setSourceFileName(job.getFileName());
                note.setSourcePageCount(job.getPageCount());
            });
        }

        noteRepo.save(note);
        log.info("Persisted note id={} for student={}", note.getId(), studentId);
    }

    private String extractTitle(String content, GenerateNoteRequest req) {
        for (String line : content.split("\n", 5)) {
            String stripped = line.replaceAll("^#+\\s*", "").trim();
            if (!stripped.isBlank() && stripped.length() <= 200) return stripped;
        }
        if (req.prompt() != null && !req.prompt().isBlank())
            return req.prompt().length() > 200 ? req.prompt().substring(0, 200) : req.prompt();
        return "Note — " + Instant.now().toString().substring(0, 10);
    }

    private void enforceRateLimit(Long studentId) {
        Instant startOfDay = Instant.now().atZone(ZoneOffset.UTC)
            .toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        long count = noteRepo.countTodayNotes(studentId, startOfDay);
        if (count >= dailyNoteLimit)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Daily note limit reached (" + dailyNoteLimit + ")");
    }

    // ── A3. Generate from audio (Whisper transcription → stream) ─────────────

    public Flux<String> generateFromAudioStream(Long studentId, String schoolName,
                                                byte[] audioBytes, String filename,
                                                String mimeType, String language) {
        enforceRateLimit(studentId);

        String lang         = language != null ? language : "en";
        String systemPrompt = "hi".equals(lang) ? systemPromptHi : systemPromptEn;
        String lengthHint   = resolveLengthHint(studentId, null);
        String fullSystem   = systemPrompt + "\n\nNote length preference: " + lengthHint;

        return aiService.transcribeAudio(audioBytes, filename, mimeType)
            .flatMapMany(transcript -> {
                if (transcript == null || transcript.isBlank()) {
                    return Flux.error(new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "TRANSCRIPTION_FAILED"));
                }

                GenerateNoteRequest synthetic = new GenerateNoteRequest(
                    "VOICE", lang, transcript, null, null, null, null, null, null, null);

                AtomicReference<StringBuilder> buffer = new AtomicReference<>(new StringBuilder());

                return aiService.generateNoteStream(fullSystem, transcript)
                    .doOnNext(delta -> buffer.get().append(delta))
                    .doOnComplete(() ->
                        persistGeneratedNote(studentId, schoolName, synthetic, lang, buffer.get().toString()))
                    .doOnError(e ->
                        log.error("Audio note stream error for student={}: {}", studentId, e.getMessage()));
            });
    }

    // ── A2. Generate from image (vision OCR → stream) ────────────────────────

    public Flux<String> generateFromImageStream(Long studentId, String schoolName,
                                                byte[] imageBytes, String mimeType,
                                                String language) {
        enforceRateLimit(studentId);

        String lang         = language != null ? language : "en";
        String systemPrompt = "hi".equals(lang) ? systemPromptHi : systemPromptEn;
        String lengthHint   = resolveLengthHint(studentId, null);
        String fullSystem   = systemPrompt + "\n\nNote length preference: " + lengthHint;

        return aiService.extractTextFromImage(imageBytes, mimeType)
            .flatMapMany(extracted -> {
                if (extracted == null || extracted.isBlank()) {
                    return Flux.error(new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "OCR_EMPTY"));
                }

                GenerateNoteRequest synthetic = new GenerateNoteRequest(
                    "PHOTO_OCR", lang, extracted, null, null, null, null, null, null, null);

                AtomicReference<StringBuilder> buffer = new AtomicReference<>(new StringBuilder());

                return aiService.generateNoteStream(fullSystem, extracted)
                    .doOnNext(delta -> buffer.get().append(delta))
                    .doOnComplete(() ->
                        persistGeneratedNote(studentId, schoolName, synthetic, lang, buffer.get().toString()))
                    .doOnError(e ->
                        log.error("Image note stream error for student={}: {}", studentId, e.getMessage()));
            });
    }

    // ── B. List notes ─────────────────────────────────────────────────────────

    public Page<NoteListItem> listNotes(Long studentId, String schoolName,
                                        String language, Long subjectId, String sourceType,
                                        Boolean isPinned, Boolean isSharedToClassroom,
                                        String search, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (search == null || search.isBlank()) {
            return noteRepo.findFilteredNoSearch(studentId, schoolName, language, subjectId,
                    sourceType, isPinned, isSharedToClassroom, pr)
                .map(this::toListItem);
        }
        return noteRepo.findFiltered(studentId, schoolName, language, subjectId,
                sourceType, isPinned, isSharedToClassroom, search, pr)
            .map(this::toListItem);
    }

    // ── C. Get note detail ────────────────────────────────────────────────────

    public NoteDetailResponse getNote(Long noteId, Long studentId, String schoolName) {
        return toDetail(findOwned(noteId, studentId, schoolName));
    }

    // ── D. Update note ────────────────────────────────────────────────────────

    @Transactional
    public NoteDetailResponse updateNote(Long noteId, Long studentId, String schoolName,
                                         UpdateNoteRequest req) {
        StudentNote note = findOwned(noteId, studentId, schoolName);
        if (!note.isActive())
            throw new ResponseStatusException(HttpStatus.GONE, "Note is in trash");

        saveVersion(note);

        if (req.title()    != null) note.setTitle(req.title());
        if (req.content()  != null) note.setContent(req.content());
        if (req.tags()     != null) note.setTags(req.tags());
        if (req.isPinned() != null) note.setPinned(req.isPinned());
        note.setEdited(true);

        return toDetail(noteRepo.save(note));
    }

    private void saveVersion(StudentNote note) {
        long count = versionRepo.countByNoteId(note.getId());
        if (count >= maxNoteVersions) {
            versionRepo.findOldestByNoteId(note.getId())
                .ifPresent(old -> versionRepo.deleteById(old.getId()));
        }
        int next = versionRepo.findMaxVersionNumber(note.getId()).orElse(0) + 1;

        NoteVersion v = new NoteVersion();
        v.setNoteId(note.getId());
        v.setVersionNumber(next);
        v.setContentSnapshot(note.getContent());
        v.setLanguage(note.getLanguage());
        versionRepo.save(v);
    }

    // ── E. Soft delete ────────────────────────────────────────────────────────

    @Transactional
    public void softDelete(Long noteId, Long studentId, String schoolName) {
        StudentNote note = findOwned(noteId, studentId, schoolName);
        note.setActive(false);
        note.setDeletedAt(Instant.now());
        noteRepo.save(note);
    }

    // ── F. Restore from trash ─────────────────────────────────────────────────

    @Transactional
    public void restore(Long noteId, Long studentId, String schoolName) {
        StudentNote note = findOwned(noteId, studentId, schoolName);
        if (note.isArchived())
            throw new ResponseStatusException(HttpStatus.GONE, "Note is archived and cannot be restored");
        note.setActive(true);
        note.setDeletedAt(null);
        note.setRestoredAt(Instant.now());
        noteRepo.save(note);
    }

    // ── G. Trash list ─────────────────────────────────────────────────────────

    public List<NoteListItem> listTrash(Long studentId, String schoolName) {
        return noteRepo.findByStudentIdAndSchoolNameAndIsActiveFalseAndIsArchivedFalse(studentId, schoolName)
            .stream().map(this::toListItem).toList();
    }

    // ── H. Pin / unpin ───────────────────────────────────────────────────────

    @Transactional
    public void setPin(Long noteId, Long studentId, String schoolName, boolean pinned) {
        StudentNote note = findOwned(noteId, studentId, schoolName);
        note.setPinned(pinned);
        noteRepo.save(note);
    }

    // ── I. Versions list ─────────────────────────────────────────────────────

    public List<NoteVersionDto> listVersions(Long noteId, Long studentId, String schoolName) {
        findOwned(noteId, studentId, schoolName);
        return versionRepo.findByNoteIdOrderByVersionNumberDesc(noteId)
            .stream()
            .map(v -> new NoteVersionDto(v.getId(), v.getVersionNumber(), v.getLanguage(),
                v.getEditedAt(), null))
            .toList();
    }

    // ── J. Restore a specific version ────────────────────────────────────────

    @Transactional
    public NoteDetailResponse restoreVersion(Long noteId, Long versionId,
                                             Long studentId, String schoolName) {
        StudentNote note = findOwned(noteId, studentId, schoolName);
        NoteVersion version = versionRepo.findById(versionId)
            .filter(v -> v.getNoteId().equals(noteId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));

        saveVersion(note);
        note.setContent(version.getContentSnapshot());
        note.setEdited(true);
        return toDetail(noteRepo.save(note));
    }

    // ── K. Stats ─────────────────────────────────────────────────────────────

    public NoteStatsResponse getStats(Long studentId, String schoolName) {
        long total = noteRepo.countActiveNotes(studentId);
        Instant startOfDay = Instant.now().atZone(ZoneOffset.UTC)
            .toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        long today = noteRepo.countTodayNotes(studentId, startOfDay);
        Map<String, Long> byLang = Map.of(
            "en", noteRepo.countByLanguage(studentId, "en"),
            "hi", noteRepo.countByLanguage(studentId, "hi"));
        Instant lastGen = noteRepo.findLastGeneratedAt(studentId).orElse(null);
        return new NoteStatsResponse(total, List.of(), byLang,
            today, Math.max(0, dailyNoteLimit - today), lastGen);
    }

    // ── L. Share to classroom ────────────────────────────────────────────────

    @Transactional
    public void shareToClassroom(Long noteId, Long studentId, String schoolName,
                                  ShareToClassroomRequest req) {
        StudentNote note = findOwned(noteId, studentId, schoolName);
        note.setSharedToClassroom(true);
        note.setSharedClassroomId(req.classroomId());
        noteRepo.save(note);
    }

    // ── M. Related notes ─────────────────────────────────────────────────────

    public List<NoteListItem> related(Long noteId, Long studentId, String schoolName) {
        StudentNote note = findOwned(noteId, studentId, schoolName);
        if (note.getSubjectId() == null) return List.of();
        return noteRepo.findRelated(studentId, note.getSubjectId(), noteId, PageRequest.of(0, 5))
            .stream().map(this::toListItem).toList();
    }

    // ── N. Preference ────────────────────────────────────────────────────────

    public NotePreferenceDto getPreference(Long studentId) {
        return prefRepo.findByStudentId(studentId)
            .map(p -> new NotePreferenceDto(p.getPreferredLanguage(), p.getPreferredNoteLength()))
            .orElse(new NotePreferenceDto("en", "STANDARD"));
    }

    @Transactional
    public NotePreferenceDto savePreference(Long studentId, NotePreferenceDto dto) {
        StudentNotePreference pref = prefRepo.findByStudentId(studentId).orElseGet(() -> {
            var p = new StudentNotePreference();
            p.setStudentId(studentId);
            return p;
        });
        if (dto.preferredLanguage()  != null) pref.setPreferredLanguage(dto.preferredLanguage());
        if (dto.preferredNoteLength() != null) pref.setPreferredNoteLength(dto.preferredNoteLength());
        prefRepo.save(pref);
        return new NotePreferenceDto(pref.getPreferredLanguage(), pref.getPreferredNoteLength());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private StudentNote findOwned(Long noteId, Long studentId, String schoolName) {
        return noteRepo.findByIdAndStudentIdAndSchoolName(noteId, studentId, schoolName)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));
    }

    private NoteListItem toListItem(StudentNote n) {
        String preview = n.getContent() != null && n.getContent().length() > 200
            ? n.getContent().substring(0, 200) : n.getContent();
        return new NoteListItem(n.getId(), n.getTitle(), n.getSubjectId(), null,
            n.getSourceType(), n.getLanguage(), n.getSourceFileName(), n.getSourcePageCount(),
            n.getTags(), n.isPinned(), n.isSharedToClassroom(), n.isEdited(),
            n.getCreatedAt(), n.getUpdatedAt(), preview, n.getDeletedAt());
    }

    private NoteDetailResponse toDetail(StudentNote n) {
        return new NoteDetailResponse(n.getId(), n.getStudentId(), n.getTitle(), n.getContent(),
            n.getSourceType(), n.getLanguage(), n.getSubjectId(), null, n.getChapterRef(),
            n.getSourceFileName(), n.getSourcePageCount(), n.getAiModelUsed(),
            n.isEdited(), n.isPinned(), n.isSharedToClassroom(), n.getSharedClassroomId(),
            n.getTags(), n.isActive(), n.getDeletedAt(), n.getRestoredAt(),
            n.isArchived(), n.getCreatedAt(), n.getUpdatedAt());
    }
}
