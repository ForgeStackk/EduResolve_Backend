package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.ContentChunk;
import com.forgeStackk.EduResolve.repository.ContentChunkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Layer 1 search: Postgres full-text. (Layer 2: pgvector / embeddings is
 * planned but kept out of this iteration to keep the stack lean.)
 *
 * GET /api/search?q=newton+law&language=en
 */
@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*")
public class SearchController {

    @Autowired private ContentChunkRepository contentRepo;

    @GetMapping
    public List<ContentChunk> search(@RequestParam String q,
                                     @RequestParam(defaultValue = "en") String language) {
        if (q == null || q.isBlank()) return List.of();
        return contentRepo.search(q.trim().toLowerCase(), language);
    }
}
