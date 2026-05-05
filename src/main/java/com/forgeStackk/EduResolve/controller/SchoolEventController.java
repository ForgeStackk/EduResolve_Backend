package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.SchoolEvent;
import com.forgeStackk.EduResolve.repository.SchoolEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class SchoolEventController {

    @Autowired
    private SchoolEventRepository repo;

    @GetMapping
    public List<SchoolEvent> list() {
        return repo.findAllByOrderByEventDateAsc();
    }

    @PostMapping
    public ResponseEntity<SchoolEvent> create(@RequestBody SchoolEvent e) {
        e.setId(null);
        return ResponseEntity.ok(repo.save(e));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
