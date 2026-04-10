package com.datapulse.service;

import com.datapulse.dto.request.ChatRequest;
import com.datapulse.dto.response.ChatResponse;
import com.datapulse.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final RestTemplate restTemplate;
    private final ConversationService conversationService;

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

    private ChatResponse fallbackResponse(ChatRequest request, String conversationId, String reason) {
        String sessionId = request.getSessionId() != null
                ? request.getSessionId()
                : UUID.randomUUID().toString();
        ChatResponse resp = new ChatResponse(reason, sessionId, "error");
        resp.setConversationId(conversationId);
        return resp;
    }
}
