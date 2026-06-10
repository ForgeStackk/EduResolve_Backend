package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.PreviousYearQuestion;
import com.forgeStackk.EduResolve.repository.PreviousYearQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pyqs")
@CrossOrigin(origins = "*")
public class PreviousYearQuestionController {

    @Autowired private PreviousYearQuestionRepository repo;

    /**
     * GET /api/pyqs?chapterId=5&difficulty=MEDIUM&year=2023
     */
    @GetMapping
    public List<PreviousYearQuestion> list(@RequestParam Long chapterId,
                                           @RequestParam(required = false) PreviousYearQuestion.Difficulty difficulty,
                                           @RequestParam(required = false) Integer year) {
        if (year != null)       return repo.findByChapterIdAndYearOrderByIdAsc(chapterId, year);
        if (difficulty != null) return repo.findByChapterIdAndDifficultyOrderByYearDesc(chapterId, difficulty);
        return repo.findByChapterIdOrderByYearDesc(chapterId);
    }

    @PostMapping
    public PreviousYearQuestion create(@RequestBody PreviousYearQuestion p) { p.setId(null); return repo.save(p); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
