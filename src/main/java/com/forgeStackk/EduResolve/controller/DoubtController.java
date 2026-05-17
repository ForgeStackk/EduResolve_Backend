package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.dto.AskDoubtRequest;
import com.forgeStackk.EduResolve.dto.DoubtFeedbackRequest;
import com.forgeStackk.EduResolve.entity.Doubt;
import com.forgeStackk.EduResolve.repository.DoubtRepository;
import com.forgeStackk.EduResolve.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doubts")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DoubtController {

    private final DoubtRepository repo;
    private final OpenAIService openAIService;

    @GetMapping
    public List<Doubt> list(@RequestParam(required = false) Long studentId) {
        if (studentId != null) return repo.findByStudentIdOrderByCreatedAtDesc(studentId);
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/ask")
    public ResponseEntity<Doubt> ask(@RequestBody AskDoubtRequest req) {
        Doubt d = new Doubt();
        d.setStudentId(req.getStudentId());
        d.setQuery(req.getQuery());
        d.setSubject(req.getSubject());
        d.setAnswer(openAIService.generateClassAppropriateAnswer(
                req.getQuery(), 9, req.getSubject(), null));
        return ResponseEntity.ok(repo.save(d));
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<Doubt> feedback(@PathVariable Long id, @RequestBody DoubtFeedbackRequest req) {
        return repo.findById(id).map(d -> {
            d.setIsHelpful(req.getIsHelpful());
            return ResponseEntity.ok(repo.save(d));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
