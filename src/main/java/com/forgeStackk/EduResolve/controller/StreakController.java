package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.repository.StudentProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/streaks")
@CrossOrigin(origins = "*")
public class StreakController {

    @Autowired
    private StudentProfileRepository repo;

    @GetMapping("/{studentId}")
    public ResponseEntity<Map<String, Object>> getStreak(@PathVariable Long studentId) {
        return repo.findById(studentId).map(s -> {
            int streak = s.getStreakDays() != null ? s.getStreakDays() : 0;
            int xp     = s.getExperiencePoints() != null ? s.getExperiencePoints() : 0;

            Map<String, Object> result = new HashMap<>();
            result.put("currentStreak",      streak);
            result.put("missionsCompleted",  xp / 50);
            result.put("gardenState",        gardenState(streak));
            return ResponseEntity.ok(result);
        }).orElse(ResponseEntity.ok(defaultStreak()));
    }

    private String gardenState(int streak) {
        if (streak >= 14) return "TREE";
        if (streak >= 7)  return "FLOWER";
        if (streak >= 3)  return "SPROUT";
        if (streak >= 1)  return "SEEDLING";
        return "WILTED";
    }

    private Map<String, Object> defaultStreak() {
        Map<String, Object> m = new HashMap<>();
        m.put("currentStreak",     0);
        m.put("missionsCompleted", 0);
        m.put("gardenState",       "WILTED");
        return m;
    }
}
