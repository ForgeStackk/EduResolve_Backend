package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.service.OpenAIService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/doubt-solver")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class DoubtSolverController {

    private final OpenAIService openAIService;

    @PostMapping("/ask")
    public ResponseEntity<DoubtAnswerResponse> ask(@RequestBody DoubtSolverRequest req) {
        int classLevel = 9;
        String answer = openAIService.generateClassAppropriateAnswer(
                req.getQuery(), classLevel, req.getSubject(), null);
        return ResponseEntity.ok(buildResponse(answer, "ai"));
    }

    @PostMapping("/image")
    public ResponseEntity<DoubtAnswerResponse> image(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "query", defaultValue = "") String query,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "studentId", required = false) Long studentId) {
        try {
            byte[] bytes = file.getBytes();
            String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
            String answer = openAIService.generateAnswerWithImage(bytes, mimeType, query, 9, subject);
            return ResponseEntity.ok(buildResponse(answer, "ai"));
        } catch (Exception e) {
            log.error("Image doubt processing failed", e);
            return ResponseEntity.internalServerError()
                    .body(buildResponse("Could not analyze the image. Please try again.", "fallback"));
        }
    }

    private DoubtAnswerResponse buildResponse(String answer, String hitType) {
        DoubtAnswerResponse r = new DoubtAnswerResponse();
        r.setId(System.currentTimeMillis());
        r.setAnswer(answer);
        r.setHitType(hitType);
        r.setSource(hitType.equals("ai") ? "AI" : "FALLBACK");
        return r;
    }

    @Data
    public static class DoubtSolverRequest {
        private String query;
        private String language;
        private String subject;
        private Long studentId;
    }

    @Data
    public static class DoubtAnswerResponse {
        private long id;
        private String answer;
        private String hitType;
        private String source;
    }
}
