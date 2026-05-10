package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.QuestionBank;
import com.forgeStackk.EduResolve.repository.QuestionBankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ncert/quiz")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QuizController {

    private final QuestionBankRepository questionBankRepository;

    @PostMapping("/generate")
    public ResponseEntity<List<QuestionBank>> generateQuiz(@RequestBody QuizRequest request) {
        List<QuestionBank> questions;

        if (request.getDifficulty() != null && !request.getDifficulty().isEmpty()) {
            QuestionBank.Difficulty difficulty = QuestionBank.Difficulty.valueOf(request.getDifficulty().toUpperCase());
            questions = questionBankRepository.findRandomQuestionsByChapterRangeAndDifficulty(
                    request.getMaxChapterId(), difficulty);
        } else {
            questions = questionBankRepository.findRandomQuestionsByChapterRange(request.getMaxChapterId());
        }

        // Limit to requested count
        List<QuestionBank> limitedQuestions = questions.stream()
                .limit(request.getCount())
                .collect(Collectors.toList());

        return ResponseEntity.ok(limitedQuestions);
    }

    @GetMapping("/chapters/{chapterId}/questions")
    public ResponseEntity<List<QuestionBank>> getQuestionsByChapter(@PathVariable Long chapterId) {
        List<QuestionBank> questions = questionBankRepository.findByChapterId(chapterId);
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/chapters/{chapterId}/questions/{difficulty}")
    public ResponseEntity<List<QuestionBank>> getQuestionsByChapterAndDifficulty(
            @PathVariable Long chapterId,
            @PathVariable QuestionBank.Difficulty difficulty) {
        List<QuestionBank> questions = questionBankRepository.findByChapterIdAndDifficulty(chapterId, difficulty);
        return ResponseEntity.ok(questions);
    }

    public static class QuizRequest {
        private String difficulty; // easy, medium, hard
        private int count;
        private Long maxChapterId;

        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public Long getMaxChapterId() { return maxChapterId; }
        public void setMaxChapterId(Long maxChapterId) { this.maxChapterId = maxChapterId; }
    }
}
