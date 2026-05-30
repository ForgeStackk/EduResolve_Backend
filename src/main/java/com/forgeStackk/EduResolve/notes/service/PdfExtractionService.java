package com.forgeStackk.EduResolve.notes.service;

import com.forgeStackk.EduResolve.notes.dto.PdfStatusResponse;
import com.forgeStackk.EduResolve.notes.entity.PdfExtractionJob;
import com.forgeStackk.EduResolve.notes.repository.PdfExtractionJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfExtractionService {

    private final PdfExtractionJobRepository jobRepo;

    @Value("${notes.pdf-storage-dir:uploads/pdf-extractions}")
    private String pdfStorageDir;

    @Value("${notes.max-pdf-pages:100}")
    private int maxPages;

    /** Saves the uploaded PDF to disk and creates a PROCESSING job row. Returns the job. */
    public PdfExtractionJob createJob(Long studentId, MultipartFile file) throws IOException {
        Path dir = Paths.get(pdfStorageDir, String.valueOf(studentId)).toAbsolutePath();
        Files.createDirectories(dir);

        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.pdf";
        String safeName = System.currentTimeMillis() + "_" + original.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path dest = dir.resolve(safeName);
        try (var in = file.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }

        PdfExtractionJob job = new PdfExtractionJob();
        job.setStudentId(studentId);
        job.setFileUrl(dest.toString());
        job.setFileName(file.getOriginalFilename());
        job.setStatus("PROCESSING");
        return jobRepo.save(job);
    }

    /** Runs asynchronously. Extracts text from the PDF stored on disk and updates the job. */
    @Async
    public void extractAsync(Long jobId) {
        PdfExtractionJob job = jobRepo.findById(jobId).orElse(null);
        if (job == null) return;

        try (PDDocument doc = Loader.loadPDF(new File(job.getFileUrl()))) {
            int totalPages = doc.getNumberOfPages();
            int pagesToProcess = Math.min(totalPages, maxPages);

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(pagesToProcess);

            StringBuilder sb = new StringBuilder();
            for (int p = 1; p <= pagesToProcess; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                sb.append("\n\n--- Page ").append(p).append(" ---\n\n");
                sb.append(stripper.getText(doc));
            }

            String extracted = sb.toString().trim();

            if (extracted.length() < 50) {
                job.setStatus("FAILED");
                job.setFailureReason("PDF_IS_IMAGE_ONLY");
            } else {
                if (totalPages > maxPages) {
                    extracted = "Note: First " + maxPages + " pages processed out of " + totalPages + ".\n\n" + extracted;
                }
                job.setStatus("COMPLETED");
                job.setExtractedText(extracted);
                job.setPageCount(pagesToProcess);
                job.setCharacterCount(extracted.length());
            }

        } catch (Exception e) {
            log.error("PDF extraction failed for jobId={}: {}", jobId, e.getMessage());
            job.setStatus("FAILED");
            job.setFailureReason("EXTRACTION_ERROR: " + e.getMessage());
        }

        job.setCompletedAt(Instant.now());
        jobRepo.save(job);
    }

    public PdfStatusResponse getStatus(Long jobId, Long studentId) {
        PdfExtractionJob job = jobRepo.findById(jobId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF job not found"));
        if (!job.getStudentId().equals(studentId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "PDF job does not belong to this student");
        return new PdfStatusResponse(job.getId(), job.getStatus(),
            job.getPageCount(), job.getCharacterCount(), job.getFailureReason());
    }
}
