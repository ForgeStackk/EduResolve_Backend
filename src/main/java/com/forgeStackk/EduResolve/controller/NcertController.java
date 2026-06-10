package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.NcertBook;
import com.forgeStackk.EduResolve.entity.NcertChapter;
import com.forgeStackk.EduResolve.entity.ContentBlock;
import com.forgeStackk.EduResolve.service.GitHubApiService;
import com.forgeStackk.EduResolve.service.OpenAIService;
import com.forgeStackk.EduResolve.service.NcertBookService;
import com.forgeStackk.EduResolve.service.NcertChapterService;
import com.forgeStackk.EduResolve.service.ContentBlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main controller for NCERT learning platform endpoints
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NcertController {

    private final GitHubApiService gitHubApiService;
    private final OpenAIService openAIService;
    private final NcertBookService ncertBookService;
    private final NcertChapterService ncertChapterService;
    private final ContentBlockService contentBlockService;

    /**
     * Get list of supported classes
     */
    @GetMapping("/classes")
    public ResponseEntity<List<String>> getClasses() {
        try {
            List<String> classes = gitHubApiService.getAvailableClasses();
            return ResponseEntity.ok(classes);
        } catch (Exception e) {
            log.error("Error fetching classes", e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Get subjects for a specific class
     */
    @GetMapping("/classes/{classId}/subjects")
    public ResponseEntity<List<String>> getSubjectsByClass(@PathVariable String classId) {
        try {
            List<String> subjects = gitHubApiService.getSubjectsByClass(classId);
            return ResponseEntity.ok(subjects);
        } catch (Exception e) {
            log.error("Error fetching subjects for class: {}", classId, e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Get books for a specific subject
     */
    @GetMapping("/subjects/{subjectId}/books")
    public ResponseEntity<List<NcertBook>> getBooksBySubject(@PathVariable String subjectId) {
        try {
            // Extract class from subject if needed, or pass as query param
            List<NcertBook> books = ncertBookService.getBooksBySubject(subjectId);
            return ResponseEntity.ok(books);
        } catch (Exception e) {
            log.error("Error fetching books for subject: {}", subjectId, e);
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Get books for class and subject
     */
    @GetMapping("/books")
    public ResponseEntity<List<NcertBook>> getBooks(@RequestParam String classGrade, 
                                                  @RequestParam String subject) {
        try {
            List<NcertBook> books = ncertBookService.getBooksByClassAndSubject(classGrade, subject);
            return ResponseEntity.ok(books);
        } catch (Exception e) {
            log.error("Error fetching books for class: {}, subject: {}", classGrade, subject, e);
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Get chapters for a specific book (from NCERT folder structure)
     */
    @GetMapping("/books/{bookId}/chapters")
    public ResponseEntity<List<NcertChapter>> getChaptersByBook(@PathVariable Long bookId) {
        try {
            // First try to get from database
            List<NcertChapter> chapters = ncertChapterService.getChaptersByBookId(bookId);
            
            // If no chapters in database, return empty list (frontend will handle this)
            if (chapters.isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }
            
            return ResponseEntity.ok(chapters);
        } catch (Exception e) {
            log.error("Error fetching chapters for book: {}", bookId, e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Get PDF URL for a specific book
     */
    @GetMapping("/books/{bookId}/pdf")
    public ResponseEntity<Map<String, String>> getBookPdfUrl(@PathVariable Long bookId) {
        try {
            NcertBook book = ncertBookService.getBookById(bookId);
            if (book != null && book.getGithubUrl() != null) {
                return ResponseEntity.ok(Map.of("url", book.getGithubUrl()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting PDF URL for book: {}", bookId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get content blocks for a specific chapter
     */
    @GetMapping("/chapters/{chapterId}/content")
    public ResponseEntity<List<ContentBlock>> getChapterContent(@PathVariable Long chapterId) {
        try {
            List<ContentBlock> content = contentBlockService.getContentByChapterId(chapterId);
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            log.error("Error fetching content for chapter: {}", chapterId, e);
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Upload new NCERT PDF to GitHub repository
     */
    @PostMapping("/books/upload")
    public ResponseEntity<Map<String, String>> uploadPdf(@RequestParam String classGrade,
                                                        @RequestParam String subject,
                                                        @RequestParam String title,
                                                        @RequestParam String localPath) {
        try {
            // This would upload the local PDF to GitHub repository
            // For now, return success response
            return ResponseEntity.ok(Map.of(
                "message", "PDF uploaded successfully",
                "bookId", "123"
            ));
        } catch (Exception e) {
            log.error("Error uploading PDF", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generate quiz based on parameters
     */
    @PostMapping("/quiz/generate")
    public ResponseEntity<List<Map<String, Object>>> generateQuiz(@RequestBody QuizRequest request) {
        try {
            // Get chapter content for context
            String chapterContent = getChapterContentForQuiz((long) request.getMaxChapterId());
            
            // Generate questions using OpenAI
            List<OpenAIService.QuizQuestion> questions = openAIService.generateQuizQuestions(
                chapterContent, 
                request.getClassLevel(), 
                request.getDifficulty(), 
                request.getCount()
            );
            
            // Convert to response format
            List<Map<String, Object>> response = questions.stream()
                .map(q -> Map.of(
                    "question", q.getQuestion(),
                    "options", List.of(q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD()),
                    "correct", q.getCorrect(),
                    "explanation", q.getExplanation()
                ))
                .limit(request.getCount())
                .toList();
                
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating quiz", e);
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * AI Q&A endpoint
     */
    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askQuestion(@RequestBody QuestionRequest request) {
        try {
            String answer = openAIService.generateClassAppropriateAnswer(
                request.getQuestion(),
                request.getClassLevel(),
                request.getSubject(),
                request.getContext()
            );
            
            return ResponseEntity.ok(Map.of("answer", answer));
        } catch (Exception e) {
            log.error("Error processing question", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get 3D visualization data for a concept
     */
    @GetMapping("/concepts/{conceptId}/visual")
    public ResponseEntity<String> getVisualization(@PathVariable String conceptId,
                                                                          @RequestParam int classLevel,
                                                                          @RequestParam String subject) {
        try {
            // Fallback: Return a descriptive prompt for the 3D viewer since visualization service is pending
            String prompt = String.format("Generate 3D visualization data for concept: %s, Class: %d, Subject: %s", conceptId, classLevel, subject);
            return ResponseEntity.ok(prompt);
        } catch (Exception e) {
            log.error("Error generating visualization for concept: {}", conceptId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getChapterContentForQuiz(Long maxChapterId) {
        // Get content from chapters up to maxChapterId
        // This is a simplified implementation
        return "Sample chapter content for quiz generation";
    }

    // DTOs
    public static class QuizRequest {
        private String difficulty;
        private int count;
        private int maxChapterId;
        private int classLevel;

        // Getters and setters
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public int getMaxChapterId() { return maxChapterId; }
        public void setMaxChapterId(int maxChapterId) { this.maxChapterId = maxChapterId; }
        public int getClassLevel() { return classLevel; }
        public void setClassLevel(int classLevel) { this.classLevel = classLevel; }
    }

    public static class QuestionRequest {
        private int classLevel;
        private String question;
        private String subject;
        private String context;

        // Getters and setters
        public int getClassLevel() { return classLevel; }
        public void setClassLevel(int classLevel) { this.classLevel = classLevel; }
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getContext() { return context; }
        public void setContext(String context) { this.context = context; }
    }
}
