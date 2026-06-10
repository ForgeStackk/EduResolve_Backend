package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.dto.AskDoubtRequest;
import com.forgeStackk.EduResolve.dto.DoubtFeedbackRequest;
import com.forgeStackk.EduResolve.entity.Doubt;
import com.forgeStackk.EduResolve.repository.DoubtRepository;
import com.forgeStackk.EduResolve.service.OpenAIService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public ResponseEntity<Doubt> ask(@Valid @RequestBody AskDoubtRequest req) {
        // Use the authenticated user's ID as the student ID when available,
        // falling back to the request body value (for backward compat).
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long studentId = req.getStudentId();
        if (auth != null && auth.getPrincipal() instanceof Long uid) {
            studentId = uid;
        }
        Doubt d = new Doubt();
        d.setStudentId(studentId);
        d.setQuery(req.getQuery());
        d.setSubject(req.getSubject());
        d.setAnswer(openAIService.generateClassAppropriateAnswer(
                req.getQuery(), 9, req.getSubject(), null));
        return ResponseEntity.status(HttpStatus.CREATED).body(repo.save(d));
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
