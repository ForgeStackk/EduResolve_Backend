package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.dto.BroadcastRequest;
import com.forgeStackk.EduResolve.entity.AuditLog;
import com.forgeStackk.EduResolve.entity.Broadcast;
import com.forgeStackk.EduResolve.entity.teacher.ParentInbox;
import com.forgeStackk.EduResolve.entity.teacher.Student;
import com.forgeStackk.EduResolve.repository.AuditLogRepository;
import com.forgeStackk.EduResolve.repository.BroadcastRepository;
import com.forgeStackk.EduResolve.repository.teacher.ParentInboxRepository;
import com.forgeStackk.EduResolve.repository.teacher.StudentRepository;
import jakarta.validation.Valid;
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
    @Autowired private StudentRepository    studentRepo;
    @Autowired private AuditLogRepository   auditRepo;
    @Autowired private ParentInboxRepository parentInboxRepo;

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        var all   = broadcastRepo.findAllByOrderByCreatedAtDesc();
        int total = all.size();
        int from  = Math.min(page * size, total);
        int to    = Math.min(from + size, total);

        List<Map<String, Object>> data = all.subList(from, to).stream().map(b -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id",             b.getId());
            row.put("channels",       b.getChannels());
            row.put("audienceGrades", b.getAudienceGrades());
            row.put("classId",        b.getClassId() != null ? b.getClassId().toString() : null);
            row.put("targetStudents", b.isTargetStudents());
            row.put("targetParents",  b.isTargetParents());
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
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody BroadcastRequest req) {
        List<Student> students = req.classId() != null
                ? studentRepo.findByClassId(req.classId())
                : studentRepo.findAll();

        int recipientCount = 0;
        if (req.targetStudentsOrFalse()) recipientCount += students.size();
        if (req.targetParentsOrFalse())  recipientCount += (int) students.stream()
                .filter(s -> s.getParentId() != null).count();

        Broadcast b = new Broadcast();
        String channelsStr = (req.channels() != null && !req.channels().isEmpty())
                ? String.join(",", req.channels()) : "whatsapp";
        b.setChannels(channelsStr);
        b.setClassId(req.classId());
        b.setTargetStudents(req.targetStudentsOrFalse());
        b.setTargetParents(req.targetParentsOrFalse());
        b.setMessage(req.message());
        b.setEmergency(req.isEmergencyOrFalse());
        b.setSentByName(req.sentByName() != null ? req.sentByName() : "Admin");
        b.setRecipientCount(recipientCount);
        b.setStatus("pending");
        broadcastRepo.save(b);

        if (req.targetParentsOrFalse()) {
            students.stream()
                    .filter(s -> s.getParentId() != null)
                    .forEach(s -> {
                        ParentInbox inbox = new ParentInbox();
                        inbox.setParentId(s.getParentId());
                        inbox.setBroadcastId(b.getId());
                        parentInboxRepo.save(inbox);
                    });
        }

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
            row.put("classId",        b.getClassId() != null ? b.getClassId().toString() : null);
            row.put("targetStudents", b.isTargetStudents());
            row.put("targetParents",  b.isTargetParents());
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
