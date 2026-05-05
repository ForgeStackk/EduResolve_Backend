package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.dto.ComplaintStatusUpdate;
import com.forgeStackk.EduResolve.entity.Complaint;
import com.forgeStackk.EduResolve.repository.ComplaintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "*")
public class ComplaintController {

    @Autowired
    private ComplaintRepository repo;

    @GetMapping
    public List<Complaint> list(@RequestParam(required = false) Long parentId) {
        if (parentId != null) return repo.findByParentIdOrderByCreatedAtDesc(parentId);
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Complaint> get(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Complaint> create(@RequestBody Complaint c) {
        c.setId(null);
        if (c.getStatus() == null) c.setStatus(Complaint.Status.Pending);
        return ResponseEntity.ok(repo.save(c));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Complaint> updateStatus(@PathVariable Long id, @RequestBody ComplaintStatusUpdate body) {
        return repo.findById(id).map(c -> {
            c.setStatus(body.getStatus());
            return ResponseEntity.ok(repo.save(c));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
