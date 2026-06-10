package com.forgeStackk.EduResolve.service;

import com.forgeStackk.EduResolve.entity.ContentBlock;
import com.forgeStackk.EduResolve.repository.ContentBlockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing Content Block operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentBlockService {

    private final ContentBlockRepository contentBlockRepository;
    private final PdfParsingService pdfParsingService;

    /**
     * Get all content blocks
     */
    public List<ContentBlock> getAllContentBlocks() {
        return contentBlockRepository.findAll();
    }

    /**
     * Get content block by ID
     */
    public ContentBlock getContentBlockById(Long id) {
        Optional<ContentBlock> contentBlock = contentBlockRepository.findById(id);
        return contentBlock.orElse(null);
    }

    /**
     * Get content blocks by chapter ID
     */
    public List<ContentBlock> getContentByChapterId(Long chapterId) {
        return contentBlockRepository.findByChapterIdOrderByOrderIndex(chapterId);
    }

    /**
     * Get content blocks by chapter ID and type
     */
    public List<ContentBlock> getContentByChapterIdAndType(Long chapterId, ContentBlock.BlockType blockType) {
        return contentBlockRepository.findByChapterIdAndBlockTypeOrderByOrderIndex(chapterId, blockType);
    }

    /**
     * Save a content block
     */
    public ContentBlock saveContentBlock(ContentBlock contentBlock) {
        return contentBlockRepository.save(contentBlock);
    }

    /**
     * Save multiple content blocks
     */
    public List<ContentBlock> saveContentBlocks(List<ContentBlock> contentBlocks) {
        return contentBlockRepository.saveAll(contentBlocks);
    }

    /**
     * Extract content blocks from PDF chapter and save to database
     */
    public void extractAndSaveContentBlocks(Long chapterId, byte[] pdfContent, int startPage, int endPage) {
        try {
            List<ContentBlock> contentBlocks = pdfParsingService.extractContentBlocksFromPdf(
                pdfContent, chapterId, startPage, endPage);
            
            // Delete existing content blocks for this chapter
            contentBlockRepository.deleteByChapterId(chapterId);
            
            // Save new content blocks
            contentBlockRepository.saveAll(contentBlocks);
            
            log.info("Extracted and saved {} content blocks for chapter ID: {}", contentBlocks.size(), chapterId);
        } catch (Exception e) {
            log.error("Error extracting content blocks from PDF for chapter ID: {}", chapterId, e);
        }
    }

    /**
     * Get text content blocks for a chapter
     */
    public List<ContentBlock> getTextContentByChapterId(Long chapterId) {
        return getContentByChapterIdAndType(chapterId, ContentBlock.BlockType.TEXT);
    }

    /**
     * Get image content blocks for a chapter
     */
    public List<ContentBlock> getImageContentByChapterId(Long chapterId) {
        return getContentByChapterIdAndType(chapterId, ContentBlock.BlockType.IMAGE);
    }

    /**
     * Get heading content blocks for a chapter
     */
    public List<ContentBlock> getHeadingContentByChapterId(Long chapterId) {
        return getContentByChapterIdAndType(chapterId, ContentBlock.BlockType.HEADING);
    }

    /**
     * Update content block
     */
    public ContentBlock updateContentBlock(Long id, ContentBlock contentBlockDetails) {
        Optional<ContentBlock> optionalContentBlock = contentBlockRepository.findById(id);
        if (optionalContentBlock.isPresent()) {
            ContentBlock contentBlock = optionalContentBlock.get();
            contentBlock.setChapterId(contentBlockDetails.getChapterId());
            contentBlock.setBlockType(contentBlockDetails.getBlockType());
            contentBlock.setContentText(contentBlockDetails.getContentText());
            contentBlock.setImageUrl(contentBlockDetails.getImageUrl());
            contentBlock.setImageFilename(contentBlockDetails.getImageFilename());
            contentBlock.setOrderIndex(contentBlockDetails.getOrderIndex());
            contentBlock.setHeading(contentBlockDetails.getHeading());
            contentBlock.setPageNumber(contentBlockDetails.getPageNumber());
            return contentBlockRepository.save(contentBlock);
        }
        return null;
    }

    /**
     * Delete a content block
     */
    public void deleteContentBlock(Long id) {
        contentBlockRepository.deleteById(id);
    }

    /**
     * Delete content blocks by chapter ID
     */
    public void deleteContentBlocksByChapterId(Long chapterId) {
        contentBlockRepository.deleteByChapterId(chapterId);
    }

    /**
     * Search content blocks by text
     */
    public List<ContentBlock> searchContentByText(String searchText) {
        return contentBlockRepository.findByContentTextContainingIgnoreCase(searchText);
    }

    /**
     * Get content blocks for a specific page
     */
    public List<ContentBlock> getContentByPageNumber(Long chapterId, Integer pageNumber) {
        return contentBlockRepository.findByChapterIdAndPageNumberOrderByOrderIndex(chapterId, pageNumber);
    }
}
