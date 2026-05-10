package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.ContentBlock;
import com.forgeStackk.EduResolve.repository.ContentBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ncert")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ContentBlockController {

    private final ContentBlockRepository contentBlockRepository;

    @GetMapping("/chapters/{chapterId}/content")
    public ResponseEntity<List<ContentBlock>> getChapterContent(@PathVariable Long chapterId) {
        List<ContentBlock> blocks = contentBlockRepository.findByChapterIdOrderByOrderIndexAsc(chapterId);
        return ResponseEntity.ok(blocks);
    }

    @GetMapping("/chapters/{chapterId}/content/{type}")
    public ResponseEntity<List<ContentBlock>> getChapterContentByType(
            @PathVariable Long chapterId,
            @PathVariable ContentBlock.BlockType type) {
        List<ContentBlock> blocks = contentBlockRepository.findByChapterIdAndBlockTypeOrderByOrderIndexAsc(chapterId, type);
        return ResponseEntity.ok(blocks);
    }
}
