package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.ContentChunk;
import com.forgeStackk.EduResolve.repository.ContentChunkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/content")
@CrossOrigin(origins = "*")
public class ContentChunkController {

    @Autowired private ContentChunkRepository repo;

    /**
     * GET /api/content?topicId=12&language=en
     * Optional filter by chunkType (SUMMARY, EXPLANATION, EXAMPLE, IMPORTANT_QA, FLASHCARD, ONE_PAGE_NOTE).
     */
    @GetMapping
    public List<ContentChunk> list(@RequestParam Long topicId,
                                   @RequestParam(defaultValue = "en") String language,
                                   @RequestParam(required = false) ContentChunk.ChunkType chunkType) {
        if (chunkType != null) {
            return repo.findByTopicIdAndChunkTypeAndLanguage(topicId, chunkType, language);
        }
        return repo.findByTopicIdAndLanguageOrderByOrderIndexAscIdAsc(topicId, language);
    }

    @PostMapping
    public ContentChunk create(@RequestBody ContentChunk c) { c.setId(null); return repo.save(c); }

    @PutMapping("/{id}")
    public ResponseEntity<ContentChunk> update(@PathVariable Long id, @RequestBody ContentChunk c) {
        return repo.findById(id).map(existing -> {
            existing.setTitle(c.getTitle());
            existing.setBody(c.getBody());
            existing.setChunkType(c.getChunkType());
            existing.setLanguage(c.getLanguage());
            existing.setOrderIndex(c.getOrderIndex());
            return ResponseEntity.ok(repo.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
