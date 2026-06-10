package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.QuizResult;
import com.forgeStackk.EduResolve.repository.QuizResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz-results")
@CrossOrigin(origins = "*")
public class QuizResultController {

    @Autowired
    private QuizResultRepository quizResultRepository;

    @PostMapping
    public QuizResult create(@RequestBody QuizResult result) {
        result.setCompletedAt(java.time.LocalDateTime.now());
        result.setRewardPoints(calculateRewardPoints(result.getTotalQuestions(), result.getCorrectAnswers()));
        return quizResultRepository.save(result);
    }

    @GetMapping("/student/{studentId}")
    public List<QuizResult> getByStudentId(@PathVariable Long studentId) {
        return quizResultRepository.findByStudentIdOrderByCompletedAtDesc(studentId);
    }

    @GetMapping("/student/{studentId}/total-points")
    public ResponseEntity<Integer> getTotalRewardPoints(@PathVariable Long studentId) {
        List<QuizResult> results = quizResultRepository.findByStudentIdOrderByCompletedAtDesc(studentId);
        int totalPoints = results.stream().mapToInt(QuizResult::getRewardPoints).sum();
        return ResponseEntity.ok(totalPoints);
    }

    private int calculateRewardPoints(int totalQuestions, int correctAnswers) {
        if (totalQuestions == 5) {
            // Out of 5: 3 correct = 3 points
            return correctAnswers >= 3 ? 3 : 0;
        } else if (totalQuestions == 10) {
            // Out of 10: 3-5 correct = 1 point, 5-7 = 3 points, 8-10 = 5 points
            if (correctAnswers >= 8) return 5;
            if (correctAnswers >= 5) return 3;
            if (correctAnswers >= 3) return 1;
            return 0;
        } else if (totalQuestions == 20) {
            // Out of 20: 12-15 = 1 point, 16-18 = 3 points, 19-20 = 5 points
            if (correctAnswers >= 19) return 5;
            if (correctAnswers >= 16) return 3;
            if (correctAnswers >= 12) return 1;
            return 0;
        }
        return 0;
    }
}
