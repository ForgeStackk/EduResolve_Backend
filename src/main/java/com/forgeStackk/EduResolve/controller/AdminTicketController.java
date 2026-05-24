package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.AuditLog;
import com.forgeStackk.EduResolve.entity.Complaint;
import com.forgeStackk.EduResolve.entity.ComplaintReply;
import com.forgeStackk.EduResolve.repository.AuditLogRepository;
import com.forgeStackk.EduResolve.repository.ComplaintRepository;
import com.forgeStackk.EduResolve.repository.ComplaintReplyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/tickets")
@CrossOrigin(origins = "*")
public class AdminTicketController {

    @Autowired private ComplaintRepository      complaintRepo;
    @Autowired private ComplaintReplyRepository replyRepo;
    @Autowired private AuditLogRepository       auditRepo;

    private static final Map<String, Long> SLA_HOURS = Map.of(
            "critical", 4L, "high", 24L, "medium", 72L, "low", 168L);

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "20") int    size,
            @RequestParam(defaultValue = "")   String status,
            @RequestParam(defaultValue = "")   String category,
            @RequestParam(defaultValue = "")   String priority) {

        // Manual in-memory filter on pageable result
        var allTickets = complaintRepo.findAllByOrderByCreatedAtDesc();
        var filtered = allTickets.stream()
                .filter(c -> status.isBlank()   || c.getStatus().name().equalsIgnoreCase(status))
                .filter(c -> category.isBlank() || category.equalsIgnoreCase(c.getCategory()))
                .filter(c -> priority.isBlank() || priority.equalsIgnoreCase(c.getPriority()))
                .toList();

        int total = filtered.size();
        int from  = Math.min(page * size, total);
        int to    = Math.min(from + size, total);
        var paged = filtered.subList(from, to);

        Instant now = Instant.now();
        List<Map<String, Object>> data = paged.stream().map(c -> mapTicket(c, now)).toList();

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
        Complaint c = new Complaint();
        c.setSubject(body.getOrDefault("subject", "").toString());
        c.setDescription(body.getOrDefault("body", "").toString());
        c.setCategory(body.getOrDefault("category", "other").toString());
        c.setPriority(body.getOrDefault("priority", "medium").toString());
        c.setRaisedByName(body.getOrDefault("raisedByName", "Admin").toString());
        c.setRaisedByRole(body.getOrDefault("raisedByRole", "admin").toString());
        c.setStatus(Complaint.Status.Pending);
        long slaHours = SLA_HOURS.getOrDefault(c.getPriority(), 72L);
        c.setSlaDueAt(Instant.now().plus(slaHours, ChronoUnit.HOURS));
        complaintRepo.save(c);

        Map<String, Object> res = new HashMap<>();
        res.put("id", c.getId());
        return ResponseEntity.status(201).body(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable Long id) {
        return complaintRepo.findById(id).map(c -> {
            Instant now = Instant.now();
            Map<String, Object> ticket = mapTicket(c, now);
            ticket.put("description", c.getDescription());
            ticket.put("replies", replyRepo.findByComplaintIdOrderByCreatedAtAsc(id).stream().map(r -> {
                Map<String, Object> rm = new HashMap<>();
                rm.put("id",         r.getId());
                rm.put("authorName", r.getAuthorName());
                rm.put("authorRole", r.getAuthorRole());
                rm.put("body",       r.getBody());
                rm.put("createdAt",  r.getCreatedAt());
                return rm;
            }).toList());
            return ResponseEntity.ok(ticket);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return complaintRepo.findById(id).map(c -> {
            if (body.containsKey("status")) {
                String s = body.get("status").toString();
                try { c.setStatus(Complaint.Status.valueOf(s)); } catch (IllegalArgumentException ignored) {}
                if ("Resolved".equals(s) || "Closed".equals(s)) c.setResolvedAt(Instant.now());
            }
            if (body.containsKey("priority")) {
                c.setPriority(body.get("priority").toString());
                long h = SLA_HOURS.getOrDefault(c.getPriority(), 72L);
                c.setSlaDueAt(Instant.now().plus(h, ChronoUnit.HOURS));
            }
            if (body.containsKey("assigneeName")) c.setAssigneeName(body.get("assigneeName").toString());
            complaintRepo.save(c);

            AuditLog log = new AuditLog();
            log.setAction("ticket.updated");
            log.setTargetType("Complaint");
            log.setTargetId(String.valueOf(id));
            log.setDetail("Updated ticket " + id + ": " + body);
            auditRepo.save(log);

            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<Map<String, Object>> addMessage(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return complaintRepo.findById(id).map(c -> {
            ComplaintReply reply = new ComplaintReply();
            reply.setComplaintId(id);
            reply.setBody(body.getOrDefault("body", "").toString());
            reply.setAuthorName(body.getOrDefault("authorName", "Admin").toString());
            reply.setAuthorRole(body.getOrDefault("authorRole", "admin").toString());
            replyRepo.save(reply);

            if (c.getStatus() == Complaint.Status.Pending) {
                c.setStatus(Complaint.Status.InReview);
                complaintRepo.save(c);
            }

            Map<String, Object> res = new HashMap<>();
            res.put("id", reply.getId());
            return ResponseEntity.status(201).body(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> mapTicket(Complaint c, Instant now) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",           c.getId());
        m.put("raisedByName", c.getRaisedByName() != null ? c.getRaisedByName() : "Unknown");
        m.put("raisedByRole", c.getRaisedByRole() != null ? c.getRaisedByRole() : "parent");
        m.put("assigneeName", c.getAssigneeName());
        m.put("category",     c.getCategory() != null ? c.getCategory() : "other");
        m.put("subject",      c.getSubject());
        m.put("status",       c.getStatus());
        m.put("priority",     c.getPriority() != null ? c.getPriority() : "medium");
        m.put("slaDueAt",     c.getSlaDueAt());
        m.put("isSlaBreach",  c.getSlaDueAt() != null && c.getSlaDueAt().isBefore(now)
                && c.getStatus() != Complaint.Status.Resolved && c.getStatus() != Complaint.Status.Closed);
        m.put("createdAt",    c.getCreatedAt());
        return m;
    }
}
