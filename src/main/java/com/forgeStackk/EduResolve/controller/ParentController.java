package com.forgeStackk.EduResolve.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgeStackk.EduResolve.ai.service.OpenAiChatService;
import com.forgeStackk.EduResolve.entity.Complaint;
import com.forgeStackk.EduResolve.entity.Fee;
import com.forgeStackk.EduResolve.entity.LeaveApplication;
import com.forgeStackk.EduResolve.entity.ParentTeacherMessage;
import com.forgeStackk.EduResolve.repository.ComplaintRepository;
import com.forgeStackk.EduResolve.repository.FeeRepository;
import com.forgeStackk.EduResolve.repository.HomeworkRepository;
import com.forgeStackk.EduResolve.repository.LeaveApplicationRepository;
import com.forgeStackk.EduResolve.repository.ParentTeacherMessageRepository;
import com.forgeStackk.EduResolve.repository.ParentsProfileRepository;
import com.forgeStackk.EduResolve.repository.StudentPerformanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parent")
@CrossOrigin(origins = "*")
public class ParentController {

    @Autowired private FeeRepository                 feeRepo;
    @Autowired private HomeworkRepository            homeworkRepo;
    @Autowired private LeaveApplicationRepository    leaveRepo;
    @Autowired private ParentsProfileRepository      parentProfileRepo;
    @Autowired private StudentPerformanceRepository  performanceRepo;
    @Autowired private ComplaintRepository              complaintRepo;
    @Autowired private ParentTeacherMessageRepository  messageRepo;
    @Autowired private OpenAiChatService               openAiChatService;
    @Autowired private ObjectMapper                    objectMapper;

    @GetMapping("/dashboard/summary")
    public Map<String, Object> summary(@RequestParam Long userId) {
        var profile = parentProfileRepo.findByUserId(userId).orElse(null);

        long pendingFees     = 0;
        long pendingHomework = 0;

        if (profile != null) {
            if (profile.getStudentId() != null) {
                pendingFees = feeRepo.findByStudentId(profile.getStudentId()).stream()
                        .filter(f -> f.getStatus() != Fee.Status.Paid && f.getStatus() != Fee.Status.Waived)
                        .count();
            }
            if (profile.getClassName() != null) {
                LocalDate today = LocalDate.now();
                pendingHomework = homeworkRepo.findByClassNameOrderByCreatedAtDesc(profile.getClassName()).stream()
                        .filter(h -> h.getDueDate() == null || !h.getDueDate().isBefore(today))
                        .count();
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("pendingFees",     pendingFees);
        result.put("pendingHomework", pendingHomework);
        return result;
    }

    @GetMapping("/performance")
    public List<Map<String, Object>> performance(@RequestParam Long studentId) {
        List<Object[]> rows = performanceRepo.getSubjectPerformance(studentId);
        return rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("subjectName", r[0]);
            m.put("colorHex",   r[1] != null ? r[1] : "#6366f1");
            m.put("avgScore",   r[2] != null ? ((Number) r[2]).intValue() : 0);
            m.put("topicCount", r[3] != null ? ((Number) r[3]).longValue() : 0L);
            return m;
        }).toList();
    }

    @PostMapping("/leave-application")
    public ResponseEntity<LeaveApplication> submitLeave(@RequestBody LeaveApplication body) {
        body.setId(null);
        body.setStatus("Pending");
        return ResponseEntity.ok(leaveRepo.save(body));
    }

    @GetMapping("/leave-application")
    public List<LeaveApplication> listLeave(@RequestParam Long userId) {
        return leaveRepo.findByParentUserIdOrderByCreatedAtDesc(userId);
    }

    @GetMapping("/children")
    public List<Map<String, Object>> children(@RequestParam Long userId) {
        return parentProfileRepo.findAllByUserId(userId).stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id",          p.getId());
            m.put("studentId",   p.getStudentId());
            m.put("studentName", p.getStudentName());
            m.put("className",   p.getClassName());
            m.put("relation",    p.getRelation());
            m.put("initials",    p.getInitials());
            m.put("color",       p.getColor());
            return m;
        }).toList();
    }

    @PostMapping("/ai-concern")
    public ResponseEntity<Map<String, Object>> aiConcern(@RequestBody Map<String, Object> body) {
        String concern   = (String) body.getOrDefault("concern", "");
        Object pidObj    = body.get("parentId");
        Long   parentId  = pidObj != null ? Long.parseLong(pidObj.toString()) : null;
        String parentName = (String) body.getOrDefault("parentName", "Parent");

        if (concern.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String systemPrompt =
            "You are a helpful school assistant for Indian rural schools. " +
            "A parent has a concern. Respond warmly and helpfully in 2-3 sentences. " +
            "After your reply, on a new line append exactly one JSON tag: " +
            "{\"route\":\"info|ticket|urgent\",\"category\":\"Fee|Teacher|Homework|General\",\"summary\":\"<10-word summary>\"} " +
            "Route rules: 'info' = general info or guidance; 'ticket' = needs school action/follow-up; " +
            "'urgent' = safety or health issue requiring immediate attention.";

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user",   "content", concern));

        String aiReply = openAiChatService.chat(messages);

        String visibleMessage = aiReply;
        String route    = "info";
        String category = "General";
        String summary  = concern.length() > 60 ? concern.substring(0, 60) : concern;

        // Extract trailing JSON routing tag
        int jsonStart = aiReply.lastIndexOf("{\"route\":");
        if (jsonStart >= 0) {
            visibleMessage = aiReply.substring(0, jsonStart).trim();
            try {
                var tag = objectMapper.readTree(aiReply.substring(jsonStart));
                route    = tag.path("route").asText("info");
                category = tag.path("category").asText("General");
                summary  = tag.path("summary").asText(summary);
            } catch (Exception ignored) {}
        }

        boolean ticketCreated = false;
        Long    ticketId      = null;

        if (("ticket".equals(route) || "urgent".equals(route)) && parentId != null) {
            Complaint c = new Complaint();
            c.setParentId(parentId);
            c.setCategory(category);
            c.setSubject(summary);
            c.setDescription(concern);
            c.setRaisedByName(parentName);
            c.setRaisedByRole("parent");
            c.setPriority("urgent".equals(route) ? "high" : "medium");
            Complaint saved = complaintRepo.save(c);
            ticketCreated = true;
            ticketId      = saved.getId();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("message",       visibleMessage);
        result.put("route",         route);
        result.put("category",      category);
        result.put("ticketCreated", ticketCreated);
        result.put("ticketId",      ticketId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/messages")
    public List<ParentTeacherMessage> getMessages(@RequestParam Long userId) {
        return messageRepo.findByParentUserIdOrderByCreatedAtAsc(userId);
    }

    @PostMapping("/messages")
    public ResponseEntity<ParentTeacherMessage> sendMessage(@RequestBody ParentTeacherMessage body) {
        body.setId(null);
        return ResponseEntity.ok(messageRepo.save(body));
    }

    @GetMapping("/all-fees")
    public List<Fee> allFees(@RequestParam Long studentId) {
        return feeRepo.findByStudentId(studentId).stream()
                .sorted(java.util.Comparator.comparing(
                        Fee::getDueDate,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .toList();
    }
}
