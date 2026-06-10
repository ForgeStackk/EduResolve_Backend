package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.QuizQuestion;
import com.forgeStackk.EduResolve.repository.QuizQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz-questions")
@CrossOrigin(origins = "*")
public class QuizQuestionController {

    @Autowired
    private QuizQuestionRepository repo;

    @GetMapping
    public List<QuizQuestion> list(@RequestParam(required = false) String subject,
                                   @RequestParam(required = false) String chapter) {
        if (subject != null && chapter != null) return repo.findBySubjectAndChapter(subject, chapter);
        if (subject != null) return repo.findBySubject(subject);
        return repo.findAll();
    }

    @PostMapping
    public ResponseEntity<QuizQuestion> create(@RequestBody QuizQuestion q) {
        q.setId(null);
        return ResponseEntity.ok(repo.save(q));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
