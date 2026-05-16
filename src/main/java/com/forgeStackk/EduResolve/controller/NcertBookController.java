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
        NcertBook book = ncertBookService.getBookById(id);
        if (book == null) {
            return ResponseEntity.notFound().build();
        }
        return servePdf(book);
    }

    @GetMapping("/class/{classGrade}/subject/{subject}/pdf-content")
    public ResponseEntity<InputStreamResource> getPdfContentByClassAndSubject(
            @PathVariable String classGrade,
            @PathVariable String subject) {
        List<NcertBook> books = ncertBookRepository.findByClassGradeAndSubject(classGrade, subject);
        if (books.isEmpty()) {
            log.error("No book found for class: {}, subject: {}", classGrade, subject);
            return ResponseEntity.notFound().build();
        }
        return servePdf(books.get(0));
    }

    private ResponseEntity<InputStreamResource> servePdf(NcertBook book) {
        try {
            if (book.getPdfFilename() == null) {
                log.error("No pdfFilename for book id={}", book.getId());
                return ResponseEntity.notFound().build();
            }

            InputStream is = resolveInputStream(book);
            if (is == null) {
                log.error("PDF not found for book id={}, filename={}, githubPath={}",
                        book.getId(), book.getPdfFilename(), book.getGithubPath());
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", book.getPdfFilename());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(is));

        } catch (IOException e) {
            log.error("Error serving PDF for book id={}", book.getId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private InputStream resolveInputStream(NcertBook book) throws IOException {
        // 1. Absolute filesystem path stored by GitHubApiService
        String absPath = book.getGithubPath();
        if (absPath != null && !absPath.isEmpty()) {
            File f = new File(absPath);
            if (f.exists()) {
                log.info("Serving PDF from absolute path: {}", absPath);
                return new FileInputStream(f);
            }
        }

        // 2. Classpath resource — constructed from classGrade / subject / filename
        String constructed = "NCERT/NCERT " + book.getClassGrade() + "th class/"
                + book.getSubject() + " book/" + book.getPdfFilename();
        InputStream is = getClass().getClassLoader().getResourceAsStream(constructed);
        if (is != null) {
            log.info("Serving PDF from classpath (constructed): {}", constructed);
            return is;
        }

        // 3. Classpath resource — githubPath treated as classpath-relative
        if (absPath != null && !absPath.isEmpty()) {
            is = getClass().getClassLoader().getResourceAsStream(absPath);
            if (is != null) {
                log.info("Serving PDF from classpath (githubPath): {}", absPath);
                return is;
            }
        }

        // 4. Fallback: scan NCERT resource tree for a matching filename
        try {
            org.springframework.core.io.ClassPathResource base =
                    new org.springframework.core.io.ClassPathResource("NCERT");
            File baseDir = base.getFile();
            if (baseDir.exists()) {
                File found = findFile(baseDir, book.getPdfFilename());
                if (found != null) {
                    log.info("Serving PDF via filename scan: {}", found.getAbsolutePath());
                    return new FileInputStream(found);
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private File findFile(File dir, String filename) {
        File[] children = dir.listFiles();
        if (children == null) return null;
        for (File child : children) {
            if (child.isDirectory()) {
                File found = findFile(child, filename);
                if (found != null) return found;
            } else if (child.getName().equalsIgnoreCase(filename)) {
                return child;
            }
        }
        return null;
    }
}
