package com.forgeStackk.EduResolve;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.forgeStackk.EduResolve.service.OpenAIService;
import com.forgeStackk.EduResolve.service.GitHubApiService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class EduResolveApplicationTests {

    // Mocking SimpMessagingTemplate prevents context loading failures 
    // if a WebSocket broker configuration is not available during tests.
    @MockitoBean
    private SimpMessagingTemplate simpMessagingTemplate;

    @MockitoBean
    private OpenAIService openAIService;

    @MockitoBean
    private GitHubApiService gitHubApiService;

    @Test
    void contextLoads() {
    }
}
