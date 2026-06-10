package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.StudentPerformance;
import com.forgeStackk.EduResolve.repository.StudentPerformanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Tracks per-topic accuracy for the recommendation engine (rule-based,
 * not AI). Weak topics = lowest accuracy first.
 *
 *   GET  /api/performance/{studentId}
 *   GET  /api/performance/{studentId}/weak?limit=3
 *   POST /api/performance/{studentId}/record
 *      { "topicId": 12, "questionsAttempted": 5, "questionsCorrect": 3, "timeSpentSeconds": 240 }
 */
@RestController
@RequestMapping("/api/performance")
@CrossOrigin(origins = "*")
public class StudentPerformanceController {

    @Autowired private StudentPerformanceRepository repo;

    @GetMapping("/{studentId}")
    public List<StudentPerformance> all(@PathVariable Long studentId) {
        return repo.findByStudentIdOrderByAccuracyAsc(studentId);
    }

    @GetMapping("/{studentId}/weak")
    public List<StudentPerformance> weakTopics(@PathVariable Long studentId,
                                               @RequestParam(defaultValue = "3") int limit) {
        return repo.findByStudentIdOrderByAccuracyAsc(studentId).stream()
            .filter(p -> p.getQuestionsAttempted() != null && p.getQuestionsAttempted() > 0)
            .limit(Math.max(1, limit))
            .toList();
    }

    @PostMapping("/{studentId}/record")
    public StudentPerformance record(@PathVariable Long studentId, @RequestBody Map<String, Object> body) {
        Long topicId            = ((Number) body.get("topicId")).longValue();
        int  questionsAttempted = ((Number) body.getOrDefault("questionsAttempted", 0)).intValue();
        int  questionsCorrect   = ((Number) body.getOrDefault("questionsCorrect",   0)).intValue();
        long timeSpentSeconds   = ((Number) body.getOrDefault("timeSpentSeconds",   0)).longValue();

        StudentPerformance p = repo.findByStudentIdAndTopicId(studentId, topicId)
            .orElseGet(() -> {
                StudentPerformance fresh = new StudentPerformance();
                fresh.setStudentId(studentId);
                fresh.setTopicId(topicId);
                return fresh;
            });

        p.setQuestionsAttempted((p.getQuestionsAttempted() == null ? 0 : p.getQuestionsAttempted()) + questionsAttempted);
        p.setQuestionsCorrect  ((p.getQuestionsCorrect()   == null ? 0 : p.getQuestionsCorrect())   + questionsCorrect);
        p.setTimeSpentSeconds  ((p.getTimeSpentSeconds()   == null ? 0L: p.getTimeSpentSeconds())   + timeSpentSeconds);
        double accuracy = p.getQuestionsAttempted() == 0 ? 0.0
            : (p.getQuestionsCorrect() * 100.0) / p.getQuestionsAttempted();
        p.setAccuracy(Math.round(accuracy * 10.0) / 10.0);
        p.setLastAttemptAt(Instant.now());
        return repo.save(p);
    }
}
