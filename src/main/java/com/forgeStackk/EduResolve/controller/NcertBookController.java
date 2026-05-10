package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.NcertBook;
import com.forgeStackk.EduResolve.repository.NcertBookRepository;
import com.forgeStackk.EduResolve.service.GitHubApiService;
import com.forgeStackk.EduResolve.service.NcertBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/ncert")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class NcertBookController {

    private final NcertBookRepository ncertBookRepository;
    private final GitHubApiService gitHubApiService;
    private final NcertBookService ncertBookService;

    @GetMapping("/classes")
    public ResponseEntity<List<String>> getClasses() {
        try {
            List<String> classes = gitHubApiService.getAvailableClasses();
            return ResponseEntity.ok(classes);
        } catch (Exception e) {
            // Fallback to database if folder reading fails
            List<String> classes = ncertBookRepository.findDistinctClassGrades();
            return ResponseEntity.ok(classes);
        }
    }

    @GetMapping("/classes/{classGrade}/subjects")
    public ResponseEntity<List<String>> getSubjectsByClass(@PathVariable String classGrade) {
        try {
            List<String> subjects = gitHubApiService.getSubjectsByClass(classGrade);
            return ResponseEntity.ok(subjects);
        } catch (Exception e) {
            // Fallback to database if folder reading fails
            List<String> subjects = ncertBookRepository.findByClassGrade(classGrade)
                    .stream()
                    .map(NcertBook::getSubject)
                    .distinct()
                    .toList();
            return ResponseEntity.ok(subjects);
        }
    }

    @GetMapping("/subjects/{subject}/books")
    public ResponseEntity<List<NcertBook>> getBooksBySubject(@PathVariable String subject) {
        List<NcertBook> books = ncertBookRepository.findBySubject(subject);
        return ResponseEntity.ok(books);
    }

    @GetMapping("/books")
    public ResponseEntity<List<NcertBook>> getBooksByClassAndSubject(
            @RequestParam String classGrade,
            @RequestParam String subject) {
        // First try to get from database
        List<NcertBook> books = ncertBookRepository.findByClassGradeAndSubject(classGrade, subject);

        // If no books in database, fetch from filesystem and sync to DB
        if (books.isEmpty()) {
            log.info("No books found in database for class: {}, subject: {}. Fetching from filesystem...", classGrade, subject);
            List<GitHubApiService.GitHubBookInfo> githubBooks = gitHubApiService.getBooks(classGrade, subject);

            if (!githubBooks.isEmpty()) {
                // Convert GitHubBookInfo to NcertBook and save to DB
                books = githubBooks.stream()
                    .map(bookInfo -> {
                        NcertBook book = new NcertBook();
                        book.setClassGrade(classGrade);
                        book.setSubject(subject);
                        book.setTitle(bookInfo.getTitle());
                        book.setPdfFilename(bookInfo.getFilename());
                        book.setGithubPath(bookInfo.getPath());
                        return ncertBookService.saveBook(book);
                    })
                    .toList();
                log.info("Synced {} books from filesystem to database for class: {}, subject: {}", books.size(), classGrade, subject);
            }
        }

        return ResponseEntity.ok(books);
    }

    @GetMapping("/books/{id}")
    public ResponseEntity<NcertBook> getBookById(@PathVariable Long id) {
        return ncertBookRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/books/{id}/pdf")
    public ResponseEntity<NcertBook> getPdfUrl(@PathVariable Long id) {
        return ncertBookRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/books/{id}/pdf-content")
    public ResponseEntity<InputStreamResource> getPdfContent(@PathVariable Long id) {
        try {
            NcertBook book = ncertBookService.getBookById(id);
            if (book == null || book.getPdfFilename() == null) {
                return ResponseEntity.notFound().build();
            }

            // Construct PDF path
            String pdfPath = book.getGithubPath();
            if (pdfPath == null || pdfPath.isEmpty()) {
                pdfPath = "NCERT/NCERT " + book.getClassGrade() + "th class/" + book.getSubject() + " book/" + book.getPdfFilename();
            }

            // Try to read from classpath
            InputStream is = getClass().getClassLoader().getResourceAsStream(pdfPath);
            if (is == null) {
                // Try direct file path
                File file = new File(pdfPath);
                if (file.exists()) {
                    is = new FileInputStream(file);
                } else {
                    log.error("PDF file not found: {}", pdfPath);
                    return ResponseEntity.notFound().build();
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", book.getPdfFilename());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(is));

        } catch (IOException e) {
            log.error("Error serving PDF for book ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
