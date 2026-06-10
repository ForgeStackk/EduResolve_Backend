package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.Homework;
import com.forgeStackk.EduResolve.repository.HomeworkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/homework")
@CrossOrigin(origins = "*")
public class HomeworkController {

    @Autowired
    private HomeworkRepository repo;

    @GetMapping
    public List<Homework> list(@RequestParam(required = false) String className,
                               @RequestParam(required = false) Long teacherId) {
        if (className != null) return repo.findByClassNameOrderByCreatedAtDesc(className);
        if (teacherId != null) return repo.findByTeacherIdOrderByCreatedAtDesc(teacherId);
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Homework> get(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Homework> create(@RequestBody Homework hw) {
        hw.setId(null);
        return ResponseEntity.ok(repo.save(hw));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Homework> update(@PathVariable Long id, @RequestBody Homework hw) {
        return repo.findById(id).map(existing -> {
            existing.setTitle(hw.getTitle());
            existing.setDescription(hw.getDescription());
            existing.setDueDate(hw.getDueDate());
            existing.setHasAttachment(hw.isHasAttachment());
            existing.setSubject(hw.getSubject());
            existing.setClassName(hw.getClassName());
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
