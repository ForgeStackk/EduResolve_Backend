package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/audit-logs")
@CrossOrigin(origins = "*")
public class AdminAuditController {

    @Autowired private AuditLogRepository auditRepo;

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "20") int    size,
            @RequestParam(defaultValue = "")   String action,
            @RequestParam(defaultValue = "")   String actorId) {

        long actorIdLong = 0L;
        if (!actorId.isBlank()) {
            try { actorIdLong = Long.parseLong(actorId); } catch (NumberFormatException ignored) {}
        }

        var result = auditRepo.search(
                action,
                actorIdLong,
                PageRequest.of(page, size));

        List<Map<String, Object>> data = result.getContent().stream().map(l -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id",         l.getId());
            row.put("actorId",    l.getActorId());
            row.put("actorName",  l.getActorName());
            row.put("actorRole",  l.getActorRole());
            row.put("action",     l.getAction());
            row.put("targetType", l.getTargetType());
            row.put("targetId",   l.getTargetId());
            row.put("ip",         l.getIp());
            row.put("detail",     l.getDetail());
            row.put("createdAt",  l.getCreatedAt());
            return row;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("data",       data);
        response.put("total",      result.getTotalElements());
        response.put("page",       page);
        response.put("pageSize",   size);
        response.put("totalPages", result.getTotalPages());
        return response;
    }
}
