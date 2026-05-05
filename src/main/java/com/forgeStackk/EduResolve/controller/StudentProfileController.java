package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.StudentProfile;
import com.forgeStackk.EduResolve.repository.StudentProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
public class StudentProfileController {

    @Autowired
    private StudentProfileRepository repo;

    @GetMapping
    public List<StudentProfile> list(@RequestParam(required = false) String className) {
        if (className != null) return repo.findByClassName(className);
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentProfile> get(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<StudentProfile> getByUser(@PathVariable Long userId) {
        return repo.findByUserId(userId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<StudentProfile> create(@RequestBody StudentProfile s) {
        s.setId(null);
        return ResponseEntity.ok(repo.save(s));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentProfile> update(@PathVariable Long id, @RequestBody StudentProfile s) {
        return repo.findById(id).map(existing -> {
            existing.setName(s.getName());
            existing.setInitials(s.getInitials());
            existing.setColor(s.getColor());
            existing.setEngagement(s.getEngagement());
            existing.setGrade(s.getGrade());
            existing.setStatus(s.getStatus());
            existing.setClassName(s.getClassName());
            existing.setStreakDays(s.getStreakDays());
            existing.setExperiencePoints(s.getExperiencePoints());
            existing.setTopPercentage(s.getTopPercentage());
            return ResponseEntity.ok(repo.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }
}
