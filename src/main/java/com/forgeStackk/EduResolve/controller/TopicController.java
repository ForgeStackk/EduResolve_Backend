package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.Topic;
import com.forgeStackk.EduResolve.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
@CrossOrigin(origins = "*")
public class TopicController {

    @Autowired private TopicRepository repo;

    @GetMapping
    public List<Topic> list(@RequestParam(required = false) Long chapterId) {
        if (chapterId != null) return repo.findByChapterIdOrderByOrderIndexAscIdAsc(chapterId);
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Topic> get(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Topic create(@RequestBody Topic t) { t.setId(null); return repo.save(t); }

    @PutMapping("/{id}")
    public ResponseEntity<Topic> update(@PathVariable Long id, @RequestBody Topic t) {
        return repo.findById(id).map(existing -> {
            existing.setName(t.getName());
            existing.setOrderIndex(t.getOrderIndex());
            existing.setSummary(t.getSummary());
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
