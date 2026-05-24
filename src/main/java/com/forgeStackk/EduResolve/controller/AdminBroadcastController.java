package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.AuditLog;
import com.forgeStackk.EduResolve.entity.Broadcast;
import com.forgeStackk.EduResolve.repository.AuditLogRepository;
import com.forgeStackk.EduResolve.repository.BroadcastRepository;
import com.forgeStackk.EduResolve.repository.UserLoginRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/broadcasts")
@CrossOrigin(origins = "*")
public class AdminBroadcastController {

    @Autowired private BroadcastRepository  broadcastRepo;
    @Autowired private UserLoginRepository  userRepo;
    @Autowired private AuditLogRepository   auditRepo;

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        var all  = broadcastRepo.findAllByOrderByCreatedAtDesc();
        int total = all.size();
        int from  = Math.min(page * size, total);
        int to    = Math.min(from + size, total);

        List<Map<String, Object>> data = all.subList(from, to).stream().map(b -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id",             b.getId());
            row.put("channels",       b.getChannels());
            row.put("audienceGrades", b.getAudienceGrades());
            row.put("message",        b.getMessage());
            row.put("isEmergency",    b.isEmergency());
            row.put("status",         b.getStatus());
            row.put("recipientCount", b.getRecipientCount());
            row.put("sentByName",     b.getSentByName());
            row.put("scheduledAt",    b.getScheduledAt());
            row.put("sentAt",         b.getSentAt());
            row.put("createdAt",      b.getCreatedAt());
            return row;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("data",       data);
        response.put("total",      total);
        response.put("page",       page);
        response.put("pageSize",   size);
        response.put("totalPages", (int) Math.ceil((double) total / size));
        return response;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Broadcast b = new Broadcast();
        b.setChannels(body.getOrDefault("channels", "whatsapp").toString());
        b.setAudienceGrades(body.getOrDefault("audienceGrades", "").toString());
        b.setMessage(body.getOrDefault("message", "").toString());
        b.setEmergency(Boolean.parseBoolean(body.getOrDefault("isEmergency", "false").toString()));
        b.setSentByName(body.getOrDefault("sentByName", "Admin").toString());

        // Estimate recipient count: count students in matching grades
        String grades = b.getAudienceGrades();
        long recipientCount = userRepo.findAll().stream()
                .filter(u -> "student".equalsIgnoreCase(u.getRole()))
                .filter(u -> grades == null || grades.isBlank()
                        || (u.getClassName() != null && grades.contains(u.getClassName())))
                .count();
        b.setRecipientCount((int) recipientCount);
        b.setStatus("pending");
        broadcastRepo.save(b);

        AuditLog log = new AuditLog();
        log.setAction("broadcast.created");
        log.setTargetType("Broadcast");
        log.setTargetId(String.valueOf(b.getId()));
        log.setDetail("Channels: " + b.getChannels() + ", Recipients: " + recipientCount);
        auditRepo.save(log);

        Map<String, Object> res = new HashMap<>();
        res.put("id",             b.getId());
        res.put("recipientCount", recipientCount);
        return ResponseEntity.status(201).body(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable Long id) {
        return broadcastRepo.findById(id).map(b -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id",             b.getId());
            row.put("channels",       b.getChannels());
            row.put("audienceGrades", b.getAudienceGrades());
            row.put("message",        b.getMessage());
            row.put("isEmergency",    b.isEmergency());
            row.put("status",         b.getStatus());
            row.put("recipientCount", b.getRecipientCount());
            row.put("sentByName",     b.getSentByName());
            row.put("createdAt",      b.getCreatedAt());
            return ResponseEntity.ok(row);
        }).orElse(ResponseEntity.notFound().build());
    }
}
