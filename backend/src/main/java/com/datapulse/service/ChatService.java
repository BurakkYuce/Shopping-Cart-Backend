package com.datapulse.service;

import com.datapulse.dto.request.ChatRequest;
import com.datapulse.dto.response.ChatResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ChatService {

    public ChatResponse processMessage(ChatRequest request, Authentication auth) {
        return new ChatResponse(
                "Thank you for your query. The AI assistant (LangGraph integration) is coming soon. Your question: " + request.getMessage(),
                request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString(),
                "pending"
        );
    }
}
