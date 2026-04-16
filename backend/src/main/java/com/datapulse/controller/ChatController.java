package com.datapulse.controller;

import com.datapulse.dto.request.ChatRequest;
import com.datapulse.logging.LogEventPublisher;
import com.datapulse.logging.LogEventType;
import com.datapulse.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final LogEventPublisher logEventPublisher;

    @PostMapping("/ask")
    public ResponseEntity<?> ask(
            @Valid @RequestBody ChatRequest request,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        String userRole = (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty())
                ? auth.getAuthorities().iterator().next().getAuthority()
                : null;

        logEventPublisher.publish(
                LogEventType.CHAT_QUERY,
                userId,
                userRole,
                Map.of("message", request.getMessage() != null ? request.getMessage() : "")
        );

        return ResponseEntity.ok(chatService.processMessage(request, auth));
    }

    @PostMapping(value = "/ask-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(
            @Valid @RequestBody ChatRequest request,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        String userRole = (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty())
                ? auth.getAuthorities().iterator().next().getAuthority()
                : null;

        logEventPublisher.publish(
                LogEventType.CHAT_QUERY,
                userId,
                userRole,
                Map.of("message", request.getMessage() != null ? request.getMessage() : "", "stream", "true")
        );

        return chatService.processMessageStream(request, auth);
    }
}
