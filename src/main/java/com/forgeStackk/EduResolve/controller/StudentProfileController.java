package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.StudentProfile;
import com.forgeStackk.EduResolve.repository.StudentProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
public class StudentProfileController {

    @Autowired
    private StudentProfileRepository repo;

    /** List: restricted to ADMIN and TEACHER — students/parents must not see all records. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<StudentProfile> list(@RequestParam(required = false) String className) {
        if (className != null) return repo.findByClassName(className);
        return repo.findAll();
    }

    /** Get by profile ID: ADMIN/TEACHER see any; a STUDENT may only see their own record. */
    @GetMapping("/{id}")
    public ResponseEntity<StudentProfile> get(@PathVariable Long id, Authentication auth) {
        return repo.findById(id).map(profile -> {
            boolean isAdminOrTeacher = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_TEACHER"));
            if (!isAdminOrTeacher) {
                Long principalId = auth.getPrincipal() instanceof Long uid ? uid : null;
                if (principalId == null || !principalId.equals(profile.getUserId())) {
                    return ResponseEntity.status(403).<StudentProfile>build();
                }
            }
            return ResponseEntity.ok(profile);
        }).orElse(ResponseEntity.notFound().build());
    }

    /** Look up by user-login ID: only the owner or ADMIN/TEACHER. */
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<StudentProfile> getByUser(@PathVariable Long userId, Authentication auth) {
        Long principalId = auth.getPrincipal() instanceof Long uid ? uid : null;
        boolean isAdminOrTeacher = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_TEACHER"));
        if (!isAdminOrTeacher && !userId.equals(principalId)) {
            return ResponseEntity.status(403).build();
        }
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
