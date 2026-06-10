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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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

        // Always scan filesystem so missing DB records are detected and created.
        // This is the only reliable way to catch the case where the DB has a subset
        // of the books that actually exist on disk (e.g. Kshitij saved but Sparsh missing).
        List<GitHubApiService.GitHubBookInfo> githubBooks = gitHubApiService.getBooks(classGrade, subject);
        List<NcertBook> dbBooks = ncertBookRepository.findByClassGradeAndSubject(classGrade, subject);

        // Remove stale subject-level folder records (blank pdfFilename + path is a dir with subdirs).
        dbBooks.stream().filter(this::isStaleBookRecord).forEach(b -> {
            log.info("Deleting stale book record id={}, githubPath={}", b.getId(), b.getGithubPath());
            ncertBookRepository.delete(b);
        });

        if (githubBooks.isEmpty()) {
            // Nothing on filesystem — return whatever non-stale DB records remain.
            return ResponseEntity.ok(
                ncertBookRepository.findByClassGradeAndSubject(classGrade, subject)
            );
        }

        // Upsert: ensure every filesystem book has a DB record. findByGithubPath prevents duplicates.
        List<NcertBook> result = new ArrayList<>();
        for (GitHubApiService.GitHubBookInfo bookInfo : githubBooks) {
            NcertBook book = ncertBookRepository.findByGithubPath(bookInfo.getPath())
                .orElseGet(() -> {
                    NcertBook nb = new NcertBook();
                    nb.setClassGrade(classGrade);
                    nb.setSubject(subject);
                    nb.setTitle(bookInfo.getTitle());
                    nb.setPdfFilename(bookInfo.getFilename());
                    nb.setGithubPath(bookInfo.getPath());
                    return ncertBookService.saveBook(nb);
                });
            result.add(book);
        }
        log.info("Returning {} books for class={} subject={}", result.size(), classGrade, subject);
        return ResponseEntity.ok(result);
    }

    /** A stale record has a blank pdfFilename and its githubPath is a directory that contains
     *  subdirectories — i.e. a subject-level folder was saved as a single book by mistake. */
    private boolean isStaleBookRecord(NcertBook book) {
        if (book.getPdfFilename() != null && !book.getPdfFilename().isBlank()) return false;
        String path = book.getGithubPath();
        if (path == null || path.isBlank()) return false;
        File f = new File(path);
        if (!f.exists() || !f.isDirectory()) return false;
        File[] subdirs = f.listFiles(File::isDirectory);
        return subdirs != null && subdirs.length > 0;
    }

    @GetMapping("/books/{id}")
    public ResponseEntity<NcertBook> getBookById(@PathVariable Long id) {
        return ncertBookRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Returns the list of chapter PDFs inside a folder-type book (pdfFilename="").
     * Each chapter is auto-saved to ncert_book on first request so it gets a stable id
     * that the pdf-content endpoint can serve.
     */
    @GetMapping("/books/{id}/chapter-pdfs")
    public ResponseEntity<List<NcertBook>> getChapterPdfs(@PathVariable Long id) {
        NcertBook book = ncertBookService.getBookById(id);
        if (book == null) return ResponseEntity.notFound().build();

        String folderPath = book.getGithubPath();
        if (folderPath == null || folderPath.isBlank()) return ResponseEntity.ok(List.of());

        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) return ResponseEntity.ok(List.of());

        File[] pdfs = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
        if (pdfs == null || pdfs.length == 0) return ResponseEntity.ok(List.of());

        Arrays.sort(pdfs, Comparator.comparingInt(f -> extractChapterNumber(f.getName())));

        List<NcertBook> chapters = new ArrayList<>();
        for (File pdf : pdfs) {
            NcertBook chapter = ncertBookRepository.findByGithubPath(pdf.getAbsolutePath())
                .orElseGet(() -> {
                    NcertBook c = new NcertBook();
                    c.setClassGrade(book.getClassGrade());
                    c.setSubject(book.getSubject());
                    c.setTitle(formatChapterTitle(pdf.getName()));
                    c.setPdfFilename(pdf.getName());
                    c.setGithubPath(pdf.getAbsolutePath());
                    return ncertBookService.saveBook(c);
                });
            chapters.add(chapter);
        }
        return ResponseEntity.ok(chapters);
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
            if (book.getPdfFilename() == null || book.getPdfFilename().isBlank()) {
                log.error("No pdfFilename for book id={} (folder-type book, not directly servable)", book.getId());
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

    /** Extract the first integer found in a filename for numeric sort.
     *  "Chapter 10.pdf" → 10, "jehp109.pdf" → 109, "no-number.pdf" → MAX_VALUE */
    private int extractChapterNumber(String filename) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(filename);
        return m.find() ? Integer.parseInt(m.group()) : Integer.MAX_VALUE;
    }

    private String formatChapterTitle(String filename) {
        return filename.replaceAll("(?i)\\.pdf$", "").replace("_", " ").trim();
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
