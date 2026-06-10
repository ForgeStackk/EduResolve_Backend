package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.Chapter;
import com.forgeStackk.EduResolve.repository.ChapterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chapters")
@CrossOrigin(origins = "*")
public class ChapterController {

    @Autowired private ChapterRepository repo;

    @GetMapping
    public List<Chapter> list(@RequestParam(required = false) Long subjectId) {
        if (subjectId != null) return repo.findBySubjectIdOrderByOrderIndexAscIdAsc(subjectId);
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Chapter> get(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Chapter create(@RequestBody Chapter c) { c.setId(null); return repo.save(c); }

    @PutMapping("/{id}")
    public ResponseEntity<Chapter> update(@PathVariable Long id, @RequestBody Chapter c) {
        return repo.findById(id).map(existing -> {
            existing.setName(c.getName());
            existing.setOrderIndex(c.getOrderIndex());
            existing.setSummary(c.getSummary());
            existing.setEstimatedMinutes(c.getEstimatedMinutes());
            return ResponseEntity.ok(repo.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
