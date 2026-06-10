package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.repository.StudentProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaderboard")
@CrossOrigin(origins = "*")
public class LeaderboardController {

    @Autowired
    private StudentProfileRepository repo;

    @GetMapping
    public List<Map<String, Object>> getLeaderboard(@RequestParam(defaultValue = "9") String classGrade) {
        return repo.findTop10ForGrade(classGrade)
                .stream()
                .map(s -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("studentId",         s.getId());
                    entry.put("studentName",        s.getName() != null ? s.getName() : "Unknown");
                    entry.put("avatar",             s.getInitials() != null ? s.getInitials() : "?");
                    entry.put("currentStreak",      s.getStreakDays() != null ? s.getStreakDays() : 0);
                    entry.put("missionsCompleted",  s.getExperiencePoints() != null ? s.getExperiencePoints() / 50 : 0);
                    return entry;
                })
                .toList();
    }
}
