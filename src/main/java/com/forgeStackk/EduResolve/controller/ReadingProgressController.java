package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.ReadingProgress;
import com.forgeStackk.EduResolve.repository.ReadingProgressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/reading-progress")
@CrossOrigin(origins = "*")
public class ReadingProgressController {

    @Autowired
    private ReadingProgressRepository readingProgressRepository;

    @PostMapping
    public ReadingProgress create(@RequestBody ReadingProgress progress) {
        progress.setLastReadAt(java.time.LocalDateTime.now());
        return readingProgressRepository.save(progress);
    }

    @GetMapping("/student/{studentId}/latest")
    public ResponseEntity<ReadingProgress> getLatest(@PathVariable Long studentId) {
        Optional<ReadingProgress> progress = readingProgressRepository.findFirstByStudentIdOrderByLastReadAtDesc(studentId);
        return progress.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
