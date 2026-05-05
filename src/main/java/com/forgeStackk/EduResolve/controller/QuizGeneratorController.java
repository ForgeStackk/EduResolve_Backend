package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.QuizQuestion;
import com.forgeStackk.EduResolve.repository.QuizQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quiz engine. Generates a quiz purely from the database (no AI).
 *
 * POST /api/quiz/generate
 * {
 *   "chapterId": 5,
 *   "difficulty": "MEDIUM",        // EASY | MEDIUM | HARD (optional)
 *   "language":   "en",            // en | hi (optional, default en)
 *   "count":      10               // number of questions (default 10)
 * }
 */
@RestController
@RequestMapping("/api/quiz")
@CrossOrigin(origins = "*")
public class QuizGeneratorController {

    @Autowired private QuizQuestionRepository repo;

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generate(@RequestBody GenerateRequest req) {
        int count = req.count == null || req.count <= 0 ? 10 : Math.min(req.count, 50);
        String lang = req.language == null ? "en" : req.language;
        String diff = req.difficulty == null ? null : req.difficulty.name();

        List<QuizQuestion> picked = repo.generate(req.chapterId, diff, lang, count);

        Map<String, Object> response = new HashMap<>();
        response.put("chapterId", req.chapterId);
        response.put("difficulty", req.difficulty);
        response.put("language",  lang);
        response.put("requested", count);
        response.put("returned",  picked.size());
        // Recommended timer: 60s per question (tunable on client).
        response.put("recommendedDurationSeconds", picked.size() * 60);
        response.put("questions", picked);
        return ResponseEntity.ok(response);
    }

    public static class GenerateRequest {
        public Long chapterId;
        public QuizQuestion.Difficulty difficulty;
        public String language;
        public Integer count;
    }
}
