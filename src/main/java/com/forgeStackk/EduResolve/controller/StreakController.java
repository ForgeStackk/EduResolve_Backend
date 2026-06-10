package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.StudentProfile;
import com.forgeStackk.EduResolve.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/streaks")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StreakController {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final StudentProfileRepository repo;

    /** Returns current streak info without side effects. */
    @GetMapping("/{studentId}")
    public ResponseEntity<Map<String, Object>> getStreak(@PathVariable Long studentId) {
        return repo.findById(studentId)
                .map(s -> ResponseEntity.ok(buildResponse(s)))
                .orElse(ResponseEntity.ok(defaultResponse()));
    }

    /**
     * Idempotent login-day recorder.
     * Increments {@code login_days} at most once per calendar day (IST).
     * Returns the same payload shape as GET so the frontend needs only one call.
     */
    @PostMapping("/{studentId}/login")
    public ResponseEntity<Map<String, Object>> recordLogin(@PathVariable Long studentId) {
        return repo.findById(studentId).map(s -> {
            LocalDate today = LocalDate.now(IST);
            if (!today.equals(s.getLastLoginDate())) {
                s.setLoginDays((s.getLoginDays() != null ? s.getLoginDays() : 0) + 1);
                s.setLastLoginDate(today);
                repo.save(s);
            }
            return ResponseEntity.ok(buildResponse(s));
        }).orElse(ResponseEntity.ok(defaultResponse()));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> buildResponse(StudentProfile s) {
        int streak    = s.getStreakDays()       != null ? s.getStreakDays()       : 0;
        int xp        = s.getExperiencePoints() != null ? s.getExperiencePoints() : 0;
        int loginDays = s.getLoginDays()        != null ? s.getLoginDays()        : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("currentStreak",     streak);
        result.put("missionsCompleted", xp / 50);
        result.put("loginDays",         loginDays);
        result.put("gardenState",       gardenStateFromLoginDays(loginDays));
        return result;
    }

    private Map<String, Object> defaultResponse() {
        Map<String, Object> m = new HashMap<>();
        m.put("currentStreak",     0);
        m.put("missionsCompleted", 0);
        m.put("loginDays",         0);
        m.put("gardenState",       "SEED");
        return m;
    }

    /**
     * Five growth stages driven by cumulative login days:
     *   SEED     :  0 – 2   days  🌰
     *   SPROUT   :  3 – 7   days  🌱
     *   SEEDLING :  8 – 20  days  🌿
     *   BLOOM    : 21 – 50  days  🌸
     *   TREE     : 51+      days  🌳
     */
    private String gardenStateFromLoginDays(int loginDays) {
        if (loginDays >= 51) return "TREE";
        if (loginDays >= 21) return "BLOOM";
        if (loginDays >= 8)  return "SEEDLING";
        if (loginDays >= 3)  return "SPROUT";
        return "SEED";
    }
}
