package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.Subject;
import com.forgeStackk.EduResolve.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@CrossOrigin(origins = "*")
public class SubjectController {

    @Autowired private SubjectRepository repo;

    @GetMapping
    public List<Subject> list(@RequestParam(required = false) String grade) {
        if (grade != null) return repo.findByGradeOrderByName(grade);
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Subject> get(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Subject create(@RequestBody Subject s) { s.setId(null); return repo.save(s); }

    @PutMapping("/{id}")
    public ResponseEntity<Subject> update(@PathVariable Long id, @RequestBody Subject s) {
        return repo.findById(id).map(existing -> {
            existing.setName(s.getName());
            existing.setGrade(s.getGrade());
            existing.setIcon(s.getIcon());
            existing.setColorHex(s.getColorHex());
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
