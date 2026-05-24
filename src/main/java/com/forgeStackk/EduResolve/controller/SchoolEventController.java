package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.EventRsvp;
import com.forgeStackk.EduResolve.entity.SchoolEvent;
import com.forgeStackk.EduResolve.repository.EventRsvpRepository;
import com.forgeStackk.EduResolve.repository.SchoolEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class SchoolEventController {

    @Autowired private SchoolEventRepository repo;
    @Autowired private EventRsvpRepository   rsvpRepo;

    @GetMapping
    public List<SchoolEvent> list() {
        return repo.findAllByOrderByEventDateAsc();
    }

    @GetMapping("/my-rsvps")
    public List<Long> myRsvps(@RequestParam Long userId) {
        return rsvpRepo.findByUserId(userId).stream()
                .map(EventRsvp::getEventId).toList();
    }

    @PostMapping("/{id}/rsvp")
    public ResponseEntity<Map<String, Object>> rsvp(
            @PathVariable Long id,
            @RequestParam Long userId) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        var existing = rsvpRepo.findByEventIdAndUserId(id, userId).orElse(null);
        boolean rsvped;
        if (existing != null) {
            rsvpRepo.delete(existing);
            rsvped = false;
        } else {
            EventRsvp r = new EventRsvp();
            r.setEventId(id);
            r.setUserId(userId);
            rsvpRepo.save(r);
            rsvped = true;
        }
        long total = rsvpRepo.countByEventId(id);
        return ResponseEntity.ok(Map.of("rsvped", rsvped, "totalRsvps", total));
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
