package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.ContentChunk;
import com.forgeStackk.EduResolve.entity.DoubtCache;
import com.forgeStackk.EduResolve.repository.ContentChunkRepository;
import com.forgeStackk.EduResolve.repository.DoubtCacheRepository;
import com.forgeStackk.EduResolve.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cost-aware doubt solver. Flow:
 *   1. Normalize the question + hash.
 *   2. Look up DoubtCache by hash. Hit -> increment counter, return.
 *   3. Else search ContentChunks (Postgres FTS). If a chunk matches, return
 *      it as the answer (source = DB) and cache it.
 *   4. Else, only as a last resort, call AiService.generateAnswer(...) and
 *      cache the response.
 *
 * 80-90% of student questions hit step 2 or 3 once content is curated.
 */
@RestController
@RequestMapping("/api/doubt-solver")
@CrossOrigin(origins = "*")
public class DoubtSolverController {

    @Autowired private DoubtCacheRepository cacheRepo;
    @Autowired private ContentChunkRepository contentRepo;
    @Autowired private AiService aiService;

    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestBody AskRequest req) {
        String lang = req.language == null ? "en" : req.language;
        String normalized = normalize(req.query);
        String hash = sha256(normalized + "|" + lang);

        // 1. Cache hit
        var cached = cacheRepo.findByQueryHash(hash);
        if (cached.isPresent()) {
            DoubtCache d = cached.get();
            d.setHitCount(d.getHitCount() + 1);
            d.setLastHitAt(Instant.now());
            cacheRepo.save(d);
            return result(d.getAnswer(), "cache", d.getSource(), d.getId());
        }

        // 2. DB content search (Postgres FTS)
        List<ContentChunk> hits = contentRepo.search(normalized, lang);
        if (!hits.isEmpty()) {
            ContentChunk best = hits.get(0);
            String answer = (best.getTitle() != null ? best.getTitle() + ": " : "") + best.getBody();
            DoubtCache cache = saveCache(hash, normalized, answer, lang, req.subject, "DB");
            return result(answer, "db", "DB", cache.getId());
        }

        // 3. AI fallback (only when nothing else worked)
        String aiAnswer = aiService.generateAnswer(req.query, lang);
        String source = aiService.isEnabled() ? "AI" : "FALLBACK";
        DoubtCache cache = saveCache(hash, normalized, aiAnswer, lang, req.subject, source);
        return result(aiAnswer, "ai", source, cache.getId());
    }

    private DoubtCache saveCache(String hash, String normalized, String answer,
                                 String lang, String subject, String source) {
        DoubtCache c = new DoubtCache();
        c.setQueryHash(hash);
        c.setNormalizedQuery(normalized);
        c.setAnswer(answer);
        c.setLanguage(lang);
        c.setSubject(subject);
        c.setSource(source);
        c.setHitCount(1);
        c.setLastHitAt(Instant.now());
        return cacheRepo.save(c);
    }

    private Map<String, Object> result(String answer, String hitType, String source, Long id) {
        Map<String, Object> r = new HashMap<>();
        r.put("id", id);
        r.put("answer", answer);
        r.put("hitType", hitType);   // cache | db | ai
        r.put("source", source);     // DB | AI | FALLBACK | TEACHER
        return r;
    }

    private static String normalize(String q) {
        if (q == null) return "";
        return q.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static class AskRequest {
        public String query;
        public String subject;
        public String language;
        public Long studentId; // for future per-user rate limiting
    }
}
