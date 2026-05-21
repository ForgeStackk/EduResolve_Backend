package com.forgeStackk.EduResolve;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.forgeStackk.EduResolve.service.OpenAIService;
import com.forgeStackk.EduResolve.service.GitHubApiService;
import com.forgeStackk.EduResolve.ai.service.OpenAiChatService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class EduResolveApplicationTests {

    @MockitoBean
    private SimpMessagingTemplate simpMessagingTemplate;

    @MockitoBean
    private OpenAIService openAIService;

    @MockitoBean
    private GitHubApiService gitHubApiService;

    @MockitoBean
    private OpenAiChatService openAiChatService;

    @MockitoBean
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Test
    void contextLoads() {
        assertNotNull(simpMessagingTemplate);
        assertNotNull(openAIService);
        assertNotNull(gitHubApiService);
        assertNotNull(openAiChatService);
    }
}
