package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.NcertBook;
import com.forgeStackk.EduResolve.entity.NcertChapter;
import com.forgeStackk.EduResolve.repository.NcertChapterRepository;
import com.forgeStackk.EduResolve.service.NcertBookService;
import com.forgeStackk.EduResolve.service.NcertChapterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/ncert")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class NcertChapterController {

    private final NcertChapterRepository ncertChapterRepository;
    private final NcertChapterService ncertChapterService;
    private final NcertBookService ncertBookService;

    @GetMapping("/books/{bookId}/chapters")
    public ResponseEntity<List<NcertChapter>> getChaptersByBook(@PathVariable Long bookId,
                                                               @RequestParam(required = false, defaultValue = "false") boolean refresh) {
        try {
            // First check if chapters exist in database
            List<NcertChapter> chapters = ncertChapterService.getChaptersByBookId(bookId);
            
            if (!chapters.isEmpty() && !refresh) {
                log.info("Found {} chapters in database for book ID: {}", chapters.size(), bookId);
                return ResponseEntity.ok(chapters);
            }
            
            // If refresh=true, delete existing chapters and re-extract
            if (refresh && !chapters.isEmpty()) {
                log.info("Refreshing chapters for book ID: {}", bookId);
                ncertChapterService.deleteChaptersByBookId(bookId);
            }
            
            // If no chapters in DB, extract from PDF
            log.info("No chapters found in database for book ID: {}. Extracting from PDF...", bookId);
            
            NcertBook book = ncertBookService.getBookById(bookId);
            if (book == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Read PDF file from filesystem
            byte[] pdfContent = readPdfFile(book);
            if (pdfContent != null) {
                // Extract and save chapters
                ncertChapterService.extractAndSaveChapters(bookId, pdfContent);
                
                // Fetch freshly saved chapters
                chapters = ncertChapterService.getChaptersByBookId(bookId);
                log.info("Extracted and saved {} chapters from PDF for book ID: {}", chapters.size(), bookId);
            } else {
                log.warn("Could not read PDF file for book ID: {}. Creating placeholder chapters.", bookId);
                // Create and save placeholder chapters if PDF can't be read
                chapters = createAndSavePlaceholderChapters(bookId);
            }
            
            return ResponseEntity.ok(chapters);
        } catch (Exception e) {
            log.error("Error fetching chapters for book: {}", bookId, e);
            return ResponseEntity.ok(createAndSavePlaceholderChapters(bookId));
        }
    }
    
    private byte[] readPdfFile(NcertBook book) {
        try {
            String pdfPath = book.getGithubPath();
            if (pdfPath == null || pdfPath.isEmpty()) {
                // Try to construct path from class, subject and filename
                pdfPath = "NCERT/NCERT " + book.getClassGrade() + "th class/" + book.getSubject() + " book/" + book.getPdfFilename();
            }
            
            Path path = Paths.get(pdfPath);
            if (Files.exists(path)) {
                return Files.readAllBytes(path);
            }
            
            // Try classpath resource
            InputStream is = getClass().getClassLoader().getResourceAsStream(pdfPath);
            if (is != null) {
                return is.readAllBytes();
            }
            
            log.warn("PDF file not found at path: {}", pdfPath);
            return null;
        } catch (IOException e) {
            log.error("Error reading PDF file for book: {}", book.getId(), e);
            return null;
        }
    }
    
    private List<NcertChapter> createAndSavePlaceholderChapters(Long bookId) {
        List<NcertChapter> chapters = java.util.stream.IntStream.rangeClosed(1, 8)
            .mapToObj(i -> {
                NcertChapter chapter = new NcertChapter();
                chapter.setBookId(bookId);
                chapter.setChapterNumber(i);
                chapter.setTitle("Chapter " + i);
                chapter.setOrderIndex(i);
                chapter.setSummary("Chapter content available in PDF");
                // Estimate ~20 pages per chapter so the viewer opens at the right section
                chapter.setStartPage((i - 1) * 20 + 1);
                chapter.setEndPage(i * 20);
                return chapter;
            })
            .toList();
        return ncertChapterService.saveChapters(chapters);
    }

    @GetMapping("/chapters/{id}")
    public ResponseEntity<NcertChapter> getChapterById(@PathVariable Long id) {
        return ncertChapterRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
