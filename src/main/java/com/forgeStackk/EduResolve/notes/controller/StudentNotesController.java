package com.forgeStackk.EduResolve.notes.controller;

import com.forgeStackk.EduResolve.notes.dto.*;
import com.forgeStackk.EduResolve.notes.service.PdfExtractionService;
import com.forgeStackk.EduResolve.notes.service.StudentNotesService;
import com.forgeStackk.EduResolve.security.StudentPortalAuthHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/student/notes")
@RequiredArgsConstructor
public class StudentNotesController {

    private final StudentNotesService    notesService;
    private final PdfExtractionService   pdfService;
    private final StudentPortalAuthHelper auth;

    @Value("${notes.max-pdf-size-mb:25}")
    private int maxPdfSizeMb;

    // ── A. Generate note (SSE) ────────────────────────────────────────────────

    @PostMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generate(@RequestBody GenerateNoteRequest req) {
        Long studentId = auth.resolveStudentProfileId();
        String school  = auth.resolveSchoolName();
        return notesService.generateStream(studentId, school, req);
    }

    // ── B. List notes ─────────────────────────────────────────────────────────

    @GetMapping
    public Page<NoteListItem> listNotes(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) Boolean isPinned,
            @RequestParam(required = false) Boolean isSharedToClassroom,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long studentId = auth.resolveStudentProfileId();
        String school  = auth.resolveSchoolName();
        return notesService.listNotes(studentId, school, language, subjectId,
            sourceType, isPinned, isSharedToClassroom, search, page, size);
    }

    // ── C. Get note detail ────────────────────────────────────────────────────

    @GetMapping("/{noteId}")
    public NoteDetailResponse getNote(@PathVariable Long noteId) {
        Long studentId = auth.resolveStudentProfileId();
        return notesService.getNote(noteId, studentId, auth.resolveSchoolName());
    }

    // ── D. Update note ────────────────────────────────────────────────────────

    @PutMapping("/{noteId}")
    public NoteDetailResponse updateNote(@PathVariable Long noteId,
                                          @RequestBody UpdateNoteRequest req) {
        Long studentId = auth.resolveStudentProfileId();
        return notesService.updateNote(noteId, studentId, auth.resolveSchoolName(), req);
    }

    // ── E. Soft delete ────────────────────────────────────────────────────────

    @DeleteMapping("/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNote(@PathVariable Long noteId) {
        Long studentId = auth.resolveStudentProfileId();
        notesService.softDelete(noteId, studentId, auth.resolveSchoolName());
    }

    // ── F. Restore from trash ─────────────────────────────────────────────────

    @PostMapping("/{noteId}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restore(@PathVariable Long noteId) {
        Long studentId = auth.resolveStudentProfileId();
        notesService.restore(noteId, studentId, auth.resolveSchoolName());
    }

    // ── G. Trash list ─────────────────────────────────────────────────────────

    @GetMapping("/trash")
    public List<NoteListItem> listTrash() {
        Long studentId = auth.resolveStudentProfileId();
        return notesService.listTrash(studentId, auth.resolveSchoolName());
    }

    // ── H. Pin ───────────────────────────────────────────────────────────────

    @PostMapping("/{noteId}/pin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void pin(@PathVariable Long noteId) {
        Long studentId = auth.resolveStudentProfileId();
        notesService.setPin(noteId, studentId, auth.resolveSchoolName(), true);
    }

    @DeleteMapping("/{noteId}/pin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unpin(@PathVariable Long noteId) {
        Long studentId = auth.resolveStudentProfileId();
        notesService.setPin(noteId, studentId, auth.resolveSchoolName(), false);
    }

    // ── I. Versions list ─────────────────────────────────────────────────────

    @GetMapping("/{noteId}/versions")
    public List<NoteVersionDto> listVersions(@PathVariable Long noteId) {
        Long studentId = auth.resolveStudentProfileId();
        return notesService.listVersions(noteId, studentId, auth.resolveSchoolName());
    }

    // ── J. Restore version ───────────────────────────────────────────────────

    @PostMapping("/{noteId}/versions/{versionId}/restore")
    public NoteDetailResponse restoreVersion(@PathVariable Long noteId,
                                              @PathVariable Long versionId) {
        Long studentId = auth.resolveStudentProfileId();
        return notesService.restoreVersion(noteId, versionId, studentId, auth.resolveSchoolName());
    }

    // ── K. Stats ─────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    public NoteStatsResponse stats() {
        Long studentId = auth.resolveStudentProfileId();
        return notesService.getStats(studentId, auth.resolveSchoolName());
    }

    // ── L. Share to classroom ────────────────────────────────────────────────

    @PostMapping("/{noteId}/share")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void shareToClassroom(@PathVariable Long noteId,
                                  @RequestBody ShareToClassroomRequest req) {
        Long studentId = auth.resolveStudentProfileId();
        notesService.shareToClassroom(noteId, studentId, auth.resolveSchoolName(), req);
    }

    // ── M. Related notes ─────────────────────────────────────────────────────

    @GetMapping("/{noteId}/related")
    public List<NoteListItem> related(@PathVariable Long noteId) {
        Long studentId = auth.resolveStudentProfileId();
        return notesService.related(noteId, studentId, auth.resolveSchoolName());
    }

    // ── N. Preference ────────────────────────────────────────────────────────

    @GetMapping("/preference")
    public NotePreferenceDto getPreference() {
        return notesService.getPreference(auth.resolveStudentProfileId());
    }

    @PutMapping("/preference")
    public NotePreferenceDto savePreference(@RequestBody NotePreferenceDto dto) {
        return notesService.savePreference(auth.resolveStudentProfileId(), dto);
    }

    // ── O. PDF upload ─────────────────────────────────────────────────────────

    @PostMapping("/pdf/upload")
    public ResponseEntity<PdfUploadResponse> uploadPdf(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        long maxBytes = (long) maxPdfSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                "PDF exceeds " + maxPdfSizeMb + " MB limit");

        Long studentId = auth.resolveStudentProfileId();
        try {
            var job = pdfService.createJob(studentId, file);
            pdfService.extractAsync(job.getId());
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new PdfUploadResponse(job.getId(), job.getFileName(), job.getStatus()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save PDF");
        }
    }

    // ── P. PDF status ─────────────────────────────────────────────────────────

    @GetMapping("/pdf/{jobId}/status")
    public PdfStatusResponse pdfStatus(@PathVariable Long jobId) {
        Long studentId = auth.resolveStudentProfileId();
        return pdfService.getStatus(jobId, studentId);
    }
}
