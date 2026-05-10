package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.ContentBlock;
import com.forgeStackk.EduResolve.entity.NcertBook;
import com.forgeStackk.EduResolve.entity.NcertChapter;
import com.forgeStackk.EduResolve.repository.ContentBlockRepository;
import com.forgeStackk.EduResolve.repository.NcertBookRepository;
import com.forgeStackk.EduResolve.repository.NcertChapterRepository;
import com.forgeStackk.EduResolve.service.PdfParsingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/ncert")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NcertUploadController {

    private final PdfParsingService pdfParsingService;
    private final NcertBookRepository ncertBookRepository;
    private final NcertChapterRepository ncertChapterRepository;
    private final ContentBlockRepository contentBlockRepository;

    @PostMapping("/books/upload")
    public ResponseEntity<UploadResponse> uploadNcertPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("classGrade") String classGrade,
            @RequestParam("subject") String subject,
            @RequestParam("title") String title) throws IOException {

        log.info("Uploading NCERT PDF: class={}, subject={}, title={}", classGrade, subject, title);

        // Check if book already exists
        if (ncertBookRepository.existsByClassGradeAndSubjectAndTitle(classGrade, subject, title)) {
            return ResponseEntity.badRequest()
                    .body(new UploadResponse(false, "Book already exists", null));
        }

        // Parse PDF metadata
        PdfParsingService.PdfMetadata metadata = pdfParsingService.extractMetadata(file);

        // Create book record
        NcertBook book = new NcertBook();
        book.setClassGrade(classGrade);
        book.setSubject(subject);
        book.setTitle(title);
        book.setPdfFilename(file.getOriginalFilename());
        book.setTotalPages(metadata.getPageCount());
        ncertBookRepository.save(book);

        // Parse PDF content
        List<ContentBlock> contentBlocks = pdfParsingService.parsePdf(file);

        // Create chapter and content blocks
        int chapterOrder = 1;
        int blockOrder = 0;
        NcertChapter currentChapter = null;

        for (ContentBlock block : contentBlocks) {
            if (block.getBlockType() == ContentBlock.BlockType.HEADING) {
                // Save previous chapter if exists
                if (currentChapter != null) {
                    ncertChapterRepository.save(currentChapter);
                }

                // Create new chapter
                currentChapter = new NcertChapter();
                currentChapter.setBookId(book.getId());
                currentChapter.setTitle(block.getContentText());
                currentChapter.setChapterNumber(chapterOrder++);
                currentChapter.setOrderIndex(chapterOrder - 1);
                currentChapter.setSummary("Chapter " + chapterOrder);
            } else if (currentChapter != null) {
                // Save content block for current chapter
                block.setChapterId(currentChapter.getId());
                block.setOrderIndex(blockOrder++);
                contentBlockRepository.save(block);
            }
        }

        // Save last chapter
        if (currentChapter != null) {
            ncertChapterRepository.save(currentChapter);
        }

        log.info("Successfully uploaded and parsed NCERT PDF: bookId={}", book.getId());

        return ResponseEntity.ok(new UploadResponse(true, "Upload successful", book.getId()));
    }

    public static class UploadResponse {
        private boolean success;
        private String message;
        private Long bookId;

        public UploadResponse(boolean success, String message, Long bookId) {
            this.success = success;
            this.message = message;
            this.bookId = bookId;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Long getBookId() { return bookId; }
    }
}
