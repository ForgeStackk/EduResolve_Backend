package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.StudentDocument;
import com.forgeStackk.EduResolve.repository.StudentDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.List;

@RestController
@RequestMapping("/api/student-docs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class StudentDocumentController {

    private static final String UPLOAD_ROOT = "uploads/student-docs";

    private final StudentDocumentRepository docRepository;

    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("studentId") Long studentId) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file provided");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body("Only PDF files are allowed");
        }

        try {
            Path dir = Paths.get(UPLOAD_ROOT, String.valueOf(studentId));
            Files.createDirectories(dir);

            // Avoid filename collisions
            String safeName = System.currentTimeMillis() + "_" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path dest = dir.resolve(safeName);
            file.transferTo(dest.toFile());

            StudentDocument doc = new StudentDocument();
            doc.setStudentId(studentId);
            doc.setOriginalName(originalName);
            doc.setFilePath(dest.toString());
            doc.setFileSize(file.getSize());
            doc.setContentType(file.getContentType());
            docRepository.save(doc);

            log.info("Student {} uploaded document: {}", studentId, originalName);
            return ResponseEntity.ok(doc);

        } catch (IOException e) {
            log.error("Failed to save student document", e);
            return ResponseEntity.internalServerError().body("Upload failed");
        }
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<StudentDocument>> listByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(docRepository.findByStudentIdOrderByUploadedAtDesc(studentId));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<InputStreamResource> serveDocument(@PathVariable Long id) {
        return docRepository.findById(id).map((StudentDocument doc) -> {
            try {
                File file = new File(doc.getFilePath());
                if (!file.exists()) {
                    return ResponseEntity.notFound().<InputStreamResource>build();
                }
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDispositionFormData("inline", doc.getOriginalName());
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(new InputStreamResource(new FileInputStream(file)));
            } catch (FileNotFoundException e) {
                log.error("Student document file missing: {}", doc.getFilePath());
                return ResponseEntity.notFound().<InputStreamResource>build();
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @RequestParam Long studentId) {
        return docRepository.findById(id).map(doc -> {
            if (!doc.getStudentId().equals(studentId)) {
                return ResponseEntity.status(403).build();
            }
            try { Files.deleteIfExists(Paths.get(doc.getFilePath())); } catch (IOException ignored) {}
            docRepository.delete(doc);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
