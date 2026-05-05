package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.dto.NoticeRequest;
import com.forgeStackk.EduResolve.entity.Notice;
import com.forgeStackk.EduResolve.repository.NoticeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@CrossOrigin(origins = "*")
public class NoticeController {

    @Autowired
    private NoticeRepository repo;

    @GetMapping
    public List<Notice> list() {
        return repo.findAllByOrderBySentAtDesc();
    }

    @PostMapping
    public ResponseEntity<Notice> send(@RequestBody NoticeRequest req) {
        Notice n = new Notice();
        n.setTargetAudience(req.getTargetAudience());
        n.setMessage(req.getMessage());
        n.setChannels(req.getChannels() == null ? "" : String.join(",", req.getChannels()));
        return ResponseEntity.ok(repo.save(n));
    }
}
