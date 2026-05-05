package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.Bookmark;
import com.forgeStackk.EduResolve.repository.BookmarkRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
@CrossOrigin(origins = "*")
public class BookmarkController {

    @Autowired private BookmarkRepository repo;

    @GetMapping
    public List<Bookmark> list(@RequestParam Long studentId) {
        return repo.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    /**
     * POST /api/bookmarks
     * Idempotent: if the same (studentId, targetType, targetId) exists, return it.
     */
    @PostMapping
    public Bookmark create(@RequestBody Bookmark b) {
        return repo.findByStudentIdAndTargetTypeAndTargetId(b.getStudentId(), b.getTargetType(), b.getTargetId())
            .orElseGet(() -> { b.setId(null); return repo.save(b); });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<Void> deleteByTarget(@RequestParam Long studentId,
                                               @RequestParam Bookmark.TargetType targetType,
                                               @RequestParam Long targetId) {
        repo.deleteByStudentIdAndTargetTypeAndTargetId(studentId, targetType, targetId);
        return ResponseEntity.noContent().build();
    }
}
