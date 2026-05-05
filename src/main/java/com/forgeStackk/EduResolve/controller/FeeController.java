package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.Fee;
import com.forgeStackk.EduResolve.repository.FeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fees")
@CrossOrigin(origins = "*")
public class FeeController {

    @Autowired
    private FeeRepository repo;

    @GetMapping
    public List<Fee> list(@RequestParam(required = false) Fee.Status status,
                          @RequestParam(required = false) Long studentId) {
        if (status != null) return repo.findByStatus(status);
        if (studentId != null) return repo.findByStudentId(studentId);
        return repo.findAll();
    }

    @PostMapping
    public ResponseEntity<Fee> create(@RequestBody Fee fee) {
        fee.setId(null);
        return ResponseEntity.ok(repo.save(fee));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Fee> update(@PathVariable Long id, @RequestBody Fee fee) {
        return repo.findById(id).map(existing -> {
            existing.setAmount(fee.getAmount());
            existing.setStatus(fee.getStatus());
            existing.setDueDate(fee.getDueDate());
            existing.setStudentName(fee.getStudentName());
            existing.setClassName(fee.getClassName());
            existing.setPhone(fee.getPhone());
            return ResponseEntity.ok(repo.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/remind")
    public ResponseEntity<Map<String, Object>> sendReminder(@PathVariable Long id) {
        return repo.findById(id).map(fee -> {
            fee.setLastReminderAt(Instant.now());
            repo.save(fee);
            return ResponseEntity.ok(Map.<String, Object>of(
                    "success", true,
                    "message", "Reminder sent to " + (fee.getPhone() != null ? fee.getPhone() : "guardian"),
                    "sentAt", fee.getLastReminderAt()
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        List<Fee> all = repo.findAll();
        double revenue = all.stream()
                .filter(f -> f.getStatus() == Fee.Status.Paid)
                .mapToDouble(f -> f.getAmount() == null ? 0.0 : f.getAmount().doubleValue())
                .sum();
        long unpaidCount = all.stream().filter(f -> f.getStatus() == Fee.Status.Unpaid).count();
        return Map.of(
                "revenue", revenue,
                "unpaidCount", unpaidCount,
                "totalRecords", all.size()
        );
    }
}
