package com.datapulse.service;

import com.datapulse.dto.request.ChatRequest;
import com.datapulse.dto.response.ChatResponse;
import com.datapulse.security.UserDetailsImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final RestTemplate restTemplate;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

    @Value("${app.chatbot.url:http://localhost:8002}")
    private String chatbotUrl;

    public ChatResponse processMessage(ChatRequest request, Authentication auth) {
        String rawToken = (String) auth.getCredentials();

        if (rawToken == null || rawToken.isBlank()) {
            log.warn("No JWT token found in authentication credentials for chat proxy");
            return fallbackResponse(request, null, "Authentication token unavailable");
        }

        UserDetailsImpl currentUser = (UserDetailsImpl) auth.getPrincipal();
        String userId = currentUser.getId();

        // --- Conversation persistence: resolve or create conversation ---
        String conversationId = request.getConversationId();
        boolean isNewConversation = (conversationId == null || conversationId.isBlank());

        if (isNewConversation) {
            conversationId = conversationService.createConversation(userId, request.getMessage());
        }

        // Save user message
        conversationService.saveUserMessage(conversationId, request.getMessage());

        // Load message history for multi-turn context
        List<Map<String, String>> history = conversationService
                .getMessageHistoryForChatbot(conversationId, userId);
        request.setMessageHistory(history);

        String sessionId = request.getSessionId() != null
                ? request.getSessionId()
                : conversationId;
        request.setSessionId(sessionId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + rawToken);

            HttpEntity<ChatRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ChatResponse> response = restTemplate.exchange(
                    chatbotUrl + "/chat/ask",
                    HttpMethod.POST,
                    entity,
                    ChatResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ChatResponse body = response.getBody();

                // Save assistant response
                conversationService.saveAssistantMessage(
                        conversationId,
                        body.getMessage(),
                        body.getPlotlyJson(),
                        body.getGeneratedSql(),
                        body.getIntent()
                );

                body.setConversationId(conversationId);
                return body;
            }

            return fallbackResponse(request, conversationId,
                    "Chatbot returned unexpected status: " + response.getStatusCode());

        } catch (RestClientException e) {
            log.error("Chatbot service unreachable at {}: {}", chatbotUrl, e.getMessage());
            return fallbackResponse(request, conversationId,
                    "The AI assistant is temporarily unavailable. Please try again shortly.");
        }
    }

    public SseEmitter processMessageStream(ChatRequest request, Authentication auth) {
        SseEmitter emitter = new SseEmitter(120_000L);

        String rawToken = (String) auth.getCredentials();
        if (rawToken == null || rawToken.isBlank()) {
            emitter.completeWithError(new RuntimeException("No JWT token"));
            return emitter;
        }

        UserDetailsImpl currentUser = (UserDetailsImpl) auth.getPrincipal();
        String userId = currentUser.getId();

        String conversationId = request.getConversationId();
        boolean isNew = (conversationId == null || conversationId.isBlank());
        if (isNew) {
            conversationId = conversationService.createConversation(userId, request.getMessage());
        }
        conversationService.saveUserMessage(conversationId, request.getMessage());

        List<Map<String, String>> history = conversationService
                .getMessageHistoryForChatbot(conversationId, userId);
        request.setMessageHistory(history);
        request.setSessionId(request.getSessionId() != null ? request.getSessionId() : conversationId);
        request.setConversationId(conversationId);

        final String convId = conversationId;

        streamExecutor.execute(() -> {
            try {
                URL url = new URL(chatbotUrl + "/chat/ask-stream");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + rawToken);
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(120_000);

                try (OutputStream os = conn.getOutputStream()) {
                    objectMapper.writeValue(os, request);
                }

                String lastMessage = null;
                String lastPlotlyJson = null;
                String lastGeneratedSql = null;
                String lastIntent = null;

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.startsWith("data: ")) continue;
                        String data = line.substring(6);

                        JsonNode node = objectMapper.readTree(data);
                        if (node.has("message") && !node.get("message").asText().isEmpty())
                            lastMessage = node.get("message").asText();
                        if (node.has("plotlyJson") && !node.get("plotlyJson").isNull())
                            lastPlotlyJson = node.get("plotlyJson").asText();
                        if (node.has("generatedSql") && !node.get("generatedSql").isNull())
                            lastGeneratedSql = node.get("generatedSql").asText();
                        if (node.has("intent") && !node.get("intent").isNull())
                            lastIntent = node.get("intent").asText();

                        emitter.send(SseEmitter.event().data(data, MediaType.APPLICATION_JSON));
                    }
                }

                if (lastMessage != null) {
                    conversationService.saveAssistantMessage(
                            convId, lastMessage, lastPlotlyJson, lastGeneratedSql, lastIntent);
                }

                emitter.send(SseEmitter.event()
                        .data("{\"node\":\"__conv__\",\"conversationId\":\"" + convId + "\"}",
                                MediaType.APPLICATION_JSON));
                emitter.complete();

            } catch (Exception e) {
                log.error("Stream proxy failed: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event().data(
                            "{\"node\":\"__error__\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}",
                            MediaType.APPLICATION_JSON));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private ChatResponse fallbackResponse(ChatRequest request, String conversationId, String reason) {
        String sessionId = request.getSessionId() != null
                ? request.getSessionId()
                : UUID.randomUUID().toString();
        ChatResponse resp = new ChatResponse(reason, sessionId, "error");
        resp.setConversationId(conversationId);
        return resp;
    }
}
