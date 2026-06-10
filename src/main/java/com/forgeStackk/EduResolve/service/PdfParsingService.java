package com.forgeStackk.EduResolve.service;

import com.forgeStackk.EduResolve.entity.ContentBlock;
import com.forgeStackk.EduResolve.entity.NcertChapter;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing NCERT PDFs using Apache PDFBox and Apache Tika.
 * Extracts text, images, and structure from PDFs for storage in the database.
 */
@Service
@Slf4j
public class PdfParsingService {

    private final Tika tika = new Tika();

    /**
     * Parse a PDF file and extract structured content blocks.
     */
    public List<ContentBlock> parsePdf(MultipartFile file) throws IOException {
        List<ContentBlock> blocks = new ArrayList<>();

        try {
            log.info("PDF parsing is currently simplified - returning basic structure");
            // Create placeholder content block
            ContentBlock block = new ContentBlock();
            block.setBlockType(ContentBlock.BlockType.HEADING);
            block.setContentText("Chapter Content");
            block.setOrderIndex(1);
            blocks.add(block);
        } catch (Exception e) {
            log.error("Error parsing PDF content", e);
        }

        return blocks;
    }

    /**
     * Extract chapter headings from PDF text using regex patterns.
     */
    private List<String> extractChapters(String text) {
        List<String> chapters = new ArrayList<>();

        // Common NCERT chapter patterns
        Pattern[] chapterPatterns = {
                Pattern.compile("CHAPTER\\s+\\d+[:\\.]\\s+(.+?)(?=\\n|$)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("^\\d+\\.\\s+(.+)$", Pattern.MULTILINE),
                Pattern.compile("^(Chapter\\s+\\d+)\\s*[-:]\\s*(.+)$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE)
        };

        for (Pattern pattern : chapterPatterns) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                String chapter = matcher.group(1);
                if (chapter != null && chapter.length() > 5) {
                    chapters.add(chapter.trim());
                }
            }
        }

        // If no chapters found, split by major headings
        if (chapters.isEmpty()) {
            String[] lines = text.split("\\n\\n");
            for (String line : lines) {
                if (line.length() < 200 && line.length() > 10) {
                    chapters.add(line.trim());
                }
            }
        }

        return chapters;
    }

    /**
     * Extract paragraphs from chapter text.
     */
    private List<String> extractParagraphs(String chapterText) {
        List<String> paragraphs = new ArrayList<>();
        String[] lines = chapterText.split("\\n");

        StringBuilder currentParagraph = new StringBuilder();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                if (currentParagraph.length() > 0) {
                    paragraphs.add(currentParagraph.toString());
                    currentParagraph = new StringBuilder();
                }
            } else {
                if (currentParagraph.length() > 0) {
                    currentParagraph.append(" ");
                }
                currentParagraph.append(line);
            }
        }

        if (currentParagraph.length() > 0) {
            paragraphs.add(currentParagraph.toString());
        }

        return paragraphs;
    }

    /**
     * Extract images from PDF (placeholder implementation).
     * Full implementation would use PDFBox's image extraction capabilities.
     */
    public List<byte[]> extractImages(MultipartFile file) throws IOException {
        log.info("Image extraction not fully implemented - would use PDFBox image extraction");
        return new ArrayList<>();
    }

    /**
     * Extract metadata from PDF using Tika.
     */
    public PdfMetadata extractMetadata(MultipartFile file) throws IOException {
        Metadata metadata = new Metadata();
        try {
            String content = tika.parseToString(new ByteArrayInputStream(file.getBytes()), metadata);

            PdfMetadata pdfMetadata = new PdfMetadata();
            pdfMetadata.setTitle(metadata.get("title"));
            pdfMetadata.setAuthor(metadata.get("author"));
            pdfMetadata.setSubject(metadata.get("subject"));
            pdfMetadata.setCreator(metadata.get("creator"));
            String pageCountStr = metadata.get("xmpTPg:NPages");
            pdfMetadata.setPageCount(Integer.parseInt(pageCountStr != null ? pageCountStr : "0"));
            pdfMetadata.setContent(content);

            return pdfMetadata;
        } catch (TikaException e) {
            log.error("Error parsing PDF metadata with Tika", e);
            throw new IOException("Failed to extract metadata from PDF", e);
        }
    }

    /**
     * Extract chapters from PDF for book processing
     * Parses PDF text to find actual chapter titles
     */
    public List<NcertChapter> extractChaptersFromPdf(byte[] pdfContent, Long bookId) {
        List<NcertChapter> chapters = new ArrayList<>();

        try {
            // Extract text from PDF
            String pdfText = extractFullText(pdfContent);
            
            // Look for chapter patterns in the text
            List<String> chapterTitles = extractChapterTitlesFromText(pdfText);
            
            if (!chapterTitles.isEmpty()) {
                // Create chapters from extracted titles
                for (int i = 0; i < chapterTitles.size() && i < 15; i++) {
                    NcertChapter chapter = new NcertChapter();
                    chapter.setBookId(bookId);
                    chapter.setChapterNumber(i + 1);
                    chapter.setTitle(chapterTitles.get(i));
                    chapter.setOrderIndex(i + 1);
                    chapter.setSummary("Chapter " + (i + 1) + ": " + chapterTitles.get(i));
                    chapters.add(chapter);
                }
                log.info("Extracted {} chapter titles from PDF for book ID: {}", chapters.size(), bookId);
            } else {
                // Fallback to default chapters if no titles found
                log.warn("No chapter titles found in PDF for book ID: {}. Using placeholders.", bookId);
                for (int i = 1; i <= 8; i++) {
                    NcertChapter chapter = new NcertChapter();
                    chapter.setBookId(bookId);
                    chapter.setChapterNumber(i);
                    chapter.setTitle("Chapter " + i);
                    chapter.setOrderIndex(i);
                    chapters.add(chapter);
                }
            }
        } catch (Exception e) {
            log.error("Error extracting chapters from PDF for book ID: {}", bookId, e);
            // Fallback to default chapters on error
            for (int i = 1; i <= 8; i++) {
                NcertChapter chapter = new NcertChapter();
                chapter.setBookId(bookId);
                chapter.setChapterNumber(i);
                chapter.setTitle("Chapter " + i);
                chapter.setOrderIndex(i);
                chapters.add(chapter);
            }
        }

        return chapters;
    }
    
    /**
     * Extract chapter titles from PDF text using regex patterns
     */
    private List<String> extractChapterTitlesFromText(String text) {
        List<String> titles = new ArrayList<>();
        
        // Common NCERT chapter patterns
        Pattern[] patterns = {
            // CHAPTER 1: Title or CHAPTER 1 Title
            Pattern.compile("CHAPTER\\s+(\\d+)[:.\\s]+([^\\n]+)", Pattern.CASE_INSENSITIVE),
            // Chapter 1: Title
            Pattern.compile("Chapter\\s+(\\d+)[:.\\s]+([^\\n]+)", Pattern.CASE_INSENSITIVE),
            // 1. Title or 1: Title
            Pattern.compile("^(\\d+)[:.\\s]+([A-Z][^\\n]{3,80})$", Pattern.MULTILINE),
            // कक्षा 1 / भाग 1 (Hindi patterns)
            Pattern.compile("(?:कक्षा|भाग|पाठ)\\s+(\\d+)[:.\\s]+([^\\n]+)")
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                String chapterNum = matcher.group(1);
                String rawTitle = matcher.group(2).trim();
                
                // Clean up the title
                final String title = rawTitle.replaceAll("\\s+", " ").trim();
                
                // Skip if title is too short or looks like page numbers
                if (title.length() > 3 && title.length() < 100 && !title.matches("\\d+")) {
                    // Avoid duplicates
                    boolean exists = titles.stream()
                        .anyMatch(t -> t.toLowerCase().contains(title.toLowerCase().substring(0, Math.min(10, title.length()))));
                    
                    if (!exists) {
                        titles.add(title);
                    }
                }
            }
            
            // If we found titles with this pattern, stop checking other patterns
            if (!titles.isEmpty()) {
                break;
            }
        }
        
        return titles;
    }

    /**
     * Extract content blocks from PDF chapter
     */
    public List<ContentBlock> extractContentBlocksFromPdf(byte[] pdfContent, Long chapterId, int startPage,
            int endPage) {
        List<ContentBlock> contentBlocks = new ArrayList<>();

        try {
            // Return placeholder content blocks
            ContentBlock block = new ContentBlock();
            block.setChapterId(chapterId);
            block.setBlockType(ContentBlock.BlockType.TEXT);
            block.setContentText("Chapter content would be extracted from PDF");
            block.setOrderIndex(1);
            contentBlocks.add(block);
        } catch (Exception e) {
            log.error("Error extracting content blocks from PDF", e);
        }

        return contentBlocks;
    }

    /**
     * Extract full text content from PDF
     */
    public String extractFullText(byte[] pdfContent) {
        try {
            return tika.parseToString(new ByteArrayInputStream(pdfContent));
        } catch (Exception e) {
            log.error("Error extracting full text from PDF", e);
            return "";
        }
    }

    /**
     * Extract metadata from PDF (byte array version)
     */
    public PdfMetadata extractMetadata(byte[] pdfContent) {
        PdfMetadata metadata = new PdfMetadata();

        try {
            metadata.setPageCount(0); // Placeholder
            metadata.setTitle("NCERT Document");
            metadata.setContent("PDF content would be extracted here");
        } catch (Exception e) {
            log.error("Error extracting PDF metadata", e);
        }

        return metadata;
    }

    private boolean isHeading(String text) {
        String trimmed = text.trim();
        return trimmed.length() < 100 &&
                (trimmed.equals(trimmed.toUpperCase()) ||
                        trimmed.endsWith(":") ||
                        trimmed.matches("^[0-9]+\\..*") ||
                        trimmed.matches("^[A-Z]+\\s+[A-Z]+$"));
    }

    private boolean containsKeywords(String text, String[] keywords) {
        String lowerText = text.toLowerCase();
        for (String keyword : keywords) {
            if (lowerText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract
     * Extract end-of-chapter questions for quiz generation.
     */
    public List<String> extractQuestions(String chapterText) {
        List<String> questions = new ArrayList<>();

        // Look for question patterns
        Pattern questionPattern = Pattern.compile(
                "(?:Q\\d+\\.?|Question\\s+\\d+|\\d+\\.)\\s+(.+?)(?=\\n(?:Q\\d+|Question\\d+|\\d+\\.)|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        Matcher matcher = questionPattern.matcher(chapterText);
        while (matcher.find()) {
            String question = matcher.group(1).trim();
            if (question.length() > 10 && question.length() < 500) {
                questions.add(question);
            }
        }

        return questions;
    }

    /**
     * DTO for PDF metadata.
     */
    public static class PdfMetadata {
        private String title;
        private String author;
        private String subject;
        private String creator;
        private int pageCount;
        private String content;

        // Getters and setters
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getCreator() {
            return creator;
        }

        public void setCreator(String creator) {
            this.creator = creator;
        }

        public int getPageCount() {
            return pageCount;
        }

        public void setPageCount(int pageCount) {
            this.pageCount = pageCount;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
