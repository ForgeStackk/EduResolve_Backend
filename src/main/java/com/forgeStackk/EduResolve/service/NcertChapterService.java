package com.forgeStackk.EduResolve.service;

import com.forgeStackk.EduResolve.entity.NcertChapter;
import com.forgeStackk.EduResolve.repository.NcertChapterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing NCERT Chapter operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NcertChapterService {

    private final NcertChapterRepository ncertChapterRepository;
    private final PdfParsingService pdfParsingService;

    /**
     * Get all chapters
     */
    public List<NcertChapter> getAllChapters() {
        return ncertChapterRepository.findAll();
    }

    /**
     * Get chapter by ID
     */
    public NcertChapter getChapterById(Long id) {
        Optional<NcertChapter> chapter = ncertChapterRepository.findById(id);
        return chapter.orElse(null);
    }

    /**
     * Get chapters by book ID
     */
    public List<NcertChapter> getChaptersByBookId(Long bookId) {
        return ncertChapterRepository.findByBookIdOrderByOrderIndex(bookId);
    }

    /**
     * Save a chapter
     */
    public NcertChapter saveChapter(NcertChapter chapter) {
        return ncertChapterRepository.save(chapter);
    }

    /**
     * Save multiple chapters
     */
    public List<NcertChapter> saveChapters(List<NcertChapter> chapters) {
        return ncertChapterRepository.saveAll(chapters);
    }

    /**
     * Extract chapters from PDF and save to database
     */
    public void extractAndSaveChapters(Long bookId, byte[] pdfContent) {
        try {
            List<NcertChapter> chapters = pdfParsingService.extractChaptersFromPdf(pdfContent, bookId);
            
            // Delete existing chapters for this book
            ncertChapterRepository.deleteByBookId(bookId);
            
            // Save new chapters
            ncertChapterRepository.saveAll(chapters);
            
            log.info("Extracted and saved {} chapters for book ID: {}", chapters.size(), bookId);
        } catch (Exception e) {
            log.error("Error extracting chapters from PDF for book ID: {}", bookId, e);
        }
    }

    /**
     * Update chapter information
     */
    public NcertChapter updateChapter(Long id, NcertChapter chapterDetails) {
        Optional<NcertChapter> optionalChapter = ncertChapterRepository.findById(id);
        if (optionalChapter.isPresent()) {
            NcertChapter chapter = optionalChapter.get();
            chapter.setBookId(chapterDetails.getBookId());
            chapter.setTitle(chapterDetails.getTitle());
            chapter.setChapterNumber(chapterDetails.getChapterNumber());
            chapter.setOrderIndex(chapterDetails.getOrderIndex());
            chapter.setSummary(chapterDetails.getSummary());
            chapter.setStartPage(chapterDetails.getStartPage());
            chapter.setEndPage(chapterDetails.getEndPage());
            return ncertChapterRepository.save(chapter);
        }
        return null;
    }

    /**
     * Delete a chapter
     */
    public void deleteChapter(Long id) {
        ncertChapterRepository.deleteById(id);
    }

    /**
     * Delete chapters by book ID
     */
    public void deleteChaptersByBookId(Long bookId) {
        ncertChapterRepository.deleteByBookId(bookId);
    }

    /**
     * Get chapter by book ID and chapter number
     */
    public NcertChapter getChapterByBookIdAndNumber(Long bookId, Integer chapterNumber) {
        return ncertChapterRepository.findByBookIdAndChapterNumber(bookId, chapterNumber)
                .orElseThrow(() -> new RuntimeException("Chapter not found with bookId: " + bookId + " and chapterNumber: " + chapterNumber));
    }
}
