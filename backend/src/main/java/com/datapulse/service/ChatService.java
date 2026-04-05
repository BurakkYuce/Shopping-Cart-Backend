package com.datapulse.service;

import com.datapulse.dto.request.ChatRequest;
import com.datapulse.dto.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final RestTemplate restTemplate;

    @Value("${app.chatbot.url:http://localhost:8002}")
    private String chatbotUrl;

    public ChatResponse processMessage(ChatRequest request, Authentication auth) {
        // Raw JWT token is stored as the credentials by JwtAuthenticationFilter
        String rawToken = (String) auth.getCredentials();

        if (rawToken == null || rawToken.isBlank()) {
            log.warn("No JWT token found in authentication credentials for chat proxy");
            return fallbackResponse(request, "Authentication token unavailable");
        }

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
                return response.getBody();
            }

            return fallbackResponse(request, "Chatbot returned unexpected status: " + response.getStatusCode());

        } catch (RestClientException e) {
            log.error("Chatbot service unreachable at {}: {}", chatbotUrl, e.getMessage());
            return fallbackResponse(request,
                    "The AI assistant is temporarily unavailable. Please try again shortly.");
        }
    }

    private ChatResponse fallbackResponse(ChatRequest request, String reason) {
        String sessionId = request.getSessionId() != null
                ? request.getSessionId()
                : UUID.randomUUID().toString();
        return new ChatResponse(reason, sessionId, "error");
    }
}
