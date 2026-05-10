package com.forgeStackk.EduResolve.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.service.OpenAiService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Service for interacting with OpenAI API to generate class-appropriate answers
 */
@Service
@Slf4j
public class OpenAIService {

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${openai.api.model:gpt-3.5-turbo}")
    private String model;

    private OpenAiService openAiService;

    /**
     * Initialize OpenAI service
     */
    private OpenAiService getOpenAiService() {
        if (openAiService == null) {
            if (openaiApiKey != null && !openaiApiKey.isEmpty()) {
                openAiService = new OpenAiService(openaiApiKey, Duration.ofSeconds(60));
            } else {
                throw new IllegalStateException("OpenAI API key not configured");
            }
        }
        return openAiService;
    }

    /**
     * Generate class-appropriate answer for a student's question
     */
    public String generateClassAppropriateAnswer(String question, int classLevel, String subject, String context) {
        try {
            String systemPrompt = buildSystemPrompt(classLevel, subject, context);
            String userPrompt = buildUserPrompt(question, classLevel);

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(
                            new com.theokanning.openai.completion.chat.ChatMessage(
                                    com.theokanning.openai.completion.chat.ChatMessageRole.SYSTEM.value(),
                                    systemPrompt
                            ),
                            new com.theokanning.openai.completion.chat.ChatMessage(
                                    com.theokanning.openai.completion.chat.ChatMessageRole.USER.value(),
                                    userPrompt
                            )
                    ))
                    .maxTokens(500)
                    .temperature(0.7)
                    .build();

            ChatCompletionResult result = getOpenAiService().createChatCompletion(request);
            String answer = result.getChoices().get(0).getMessage().getContent();
            
            log.info("Generated answer for class {} question: {}", classLevel, question.substring(0, Math.min(50, question.length())));
            return answer;
            
        } catch (Exception e) {
            log.error("Error generating answer with OpenAI", e);
            return getFallbackAnswer(question, classLevel);
        }
    }

    /**
     * Generate quiz questions based on chapter and difficulty
     */
    public List<QuizQuestion> generateQuizQuestions(String chapterContent, int classLevel, String difficulty, int count) {
        try {
            String prompt = buildQuizPrompt(chapterContent, classLevel, difficulty, count);

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(
                            new com.theokanning.openai.completion.chat.ChatMessage(
                                    com.theokanning.openai.completion.chat.ChatMessageRole.SYSTEM.value(),
                                    "You are an expert NCERT educator who creates quiz questions for students."
                            ),
                            new com.theokanning.openai.completion.chat.ChatMessage(
                                    com.theokanning.openai.completion.chat.ChatMessageRole.USER.value(),
                                    prompt
                            )
                    ))
                    .maxTokens(1000)
                    .temperature(0.6)
                    .build();

            ChatCompletionResult result = getOpenAiService().createChatCompletion(request);
            String response = result.getChoices().get(0).getMessage().getContent();
            
            return parseQuizQuestions(response);
            
        } catch (Exception e) {
            log.error("Error generating quiz questions with OpenAI", e);
            return getFallbackQuizQuestions(count);
        }
    }

    /**
     * Generate 3D visualization data for a concept
     */
    public VisualizationData generateVisualizationData(String concept, int classLevel, String subject) {
        try {
            String prompt = buildVisualizationPrompt(concept, classLevel, subject);

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(
                            new com.theokanning.openai.completion.chat.ChatMessage(
                                    com.theokanning.openai.completion.chat.ChatMessageRole.SYSTEM.value(),
                                    "You are an expert in creating 3D visualization data for educational concepts. Provide JSON data for Three.js rendering."
                            ),
                            new com.theokanning.openai.completion.chat.ChatMessage(
                                    com.theokanning.openai.completion.chat.ChatMessageRole.USER.value(),
                                    prompt
                            )
                    ))
                    .maxTokens(800)
                    .temperature(0.5)
                    .build();

            ChatCompletionResult result = getOpenAiService().createChatCompletion(request);
            String response = result.getChoices().get(0).getMessage().getContent();
            
            return parseVisualizationData(response);
            
        } catch (Exception e) {
            log.error("Error generating visualization data with OpenAI", e);
            return getFallbackVisualizationData(concept);
        }
    }

    private String buildSystemPrompt(int classLevel, String subject, String context) {
        return String.format("""
            You are an expert NCERT educator specializing in %s for Class %d students.
            
            Your role is to provide clear, age-appropriate explanations that:
            1. Match the cognitive level of Class %d students
            2. Align with NCERT curriculum standards
            3. Use simple language with relatable examples
            4. Are accurate and scientifically sound
            5. Encourage critical thinking
            
            Context: %s
            
            Provide explanations that build understanding step by step, avoiding jargon unless explained.
            """, subject, classLevel, classLevel, context != null ? context : "General NCERT curriculum");
    }

    private String buildUserPrompt(String question, int classLevel) {
        return String.format("""
            Question: %s
            
            Please provide a comprehensive answer suitable for a Class %d student.
            Include:
            1. Clear explanation in simple terms
            2. Relevant examples from daily life
            3. Key points to remember
            4. Any important formulas or definitions (if applicable)
            
            Keep the language engaging and educational.
            """, question, classLevel);
    }

    private String buildQuizPrompt(String chapterContent, int classLevel, String difficulty, int count) {
        return String.format("""
            Based on the following chapter content for Class %d, create %d multiple-choice questions with %s difficulty.
            
            Chapter Content: %s
            
            Format each question as:
            Q: [Question text]
            A) [Option A]
            B) [Option B] 
            C) [Option C]
            D) [Option D]
            Correct: [A/B/C/D]
            Explanation: [Brief explanation]
            
            Questions should test understanding, not just memorization.
            """, classLevel, count, difficulty, chapterContent);
    }

    private String buildVisualizationPrompt(String concept, int classLevel, String subject) {
        return String.format("""
            Create 3D visualization data for the concept "%s" for Class %d %s students.
            
            Provide JSON data for Three.js rendering with:
            1. Geometry type and parameters
            2. Materials and colors
            3. Position and rotation
            4. Animation data (if applicable)
            5. Labels and annotations
            
            Make it educational and visually appealing for Class %d students.
            """, concept, classLevel, subject, classLevel);
    }

    private String getFallbackAnswer(String question, int classLevel) {
        return String.format("""
            I'm sorry, I'm having trouble processing your question right now. 
            For Class %d, I recommend:
            1. Checking your NCERT textbook for this topic
            2. Asking your teacher for clarification
            3. Discussing with classmates
            4. Looking for online educational resources
            
            Please try again later or rephrase your question.
            """, classLevel);
    }

    private List<QuizQuestion> getFallbackQuizQuestions(int count) {
        // Return basic fallback questions
        return List.of();
    }

    private VisualizationData getFallbackVisualizationData(String concept) {
        VisualizationData data = new VisualizationData();
        data.setConcept(concept);
        data.setType("text");
        data.setContent("Visualization temporarily unavailable. Please check back later.");
        return data;
    }

    private List<QuizQuestion> parseQuizQuestions(String response) {
        // Parse the OpenAI response into QuizQuestion objects
        // This is a simplified implementation
        return List.of();
    }

    private VisualizationData parseVisualizationData(String response) {
        // Parse the OpenAI response into VisualizationData object
        // This is a simplified implementation
        VisualizationData data = new VisualizationData();
        data.setConcept("Generated Concept");
        data.setType("3d");
        data.setContent(response);
        return data;
    }

    @Data
    public static class QuizQuestion {
        private String question;
        private String optionA;
        private String optionB;
        private String optionC;
        private String optionD;
        private String correct;
        private String explanation;
    }

    @Data
    public static class VisualizationData {
        private String concept;
        private String type; // "3d", "2d", "text"
        private String content; // JSON data or text content
        private String description;
    }
}
