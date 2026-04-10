package com.datapulse.service;

import com.datapulse.dto.response.ConversationDetailResponse;
import com.datapulse.dto.response.ConversationResponse;
import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.model.ChatConversation;
import com.datapulse.model.ChatMessage;
import com.datapulse.repository.ChatConversationRepository;
import com.datapulse.repository.ChatMessageRepository;
import com.datapulse.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;

    private String getUserId(Authentication auth) {
        return ((UserDetailsImpl) auth.getPrincipal()).getId();
    }

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    public Page<ConversationResponse> listConversations(Authentication auth, Pageable pageable) {
        String userId = getUserId(auth);
        return conversationRepository.findByUserId(userId, pageable)
                .map(ConversationResponse::from);
    }

    public ConversationDetailResponse getConversation(String conversationId, Authentication auth) {
        String userId = getUserId(auth);
        ChatConversation conv = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation", conversationId));
        List<ChatMessage> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);
        return ConversationDetailResponse.from(conv, messages);
    }

    public List<Map<String, String>> getMessageHistoryForChatbot(String conversationId, String userId) {
        ChatConversation conv = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElse(null);
        if (conv == null) return List.of();

        List<ChatMessage> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);

        // Limit to last 20 messages to keep payload reasonable
        int start = Math.max(0, messages.size() - 20);
        return messages.subList(start, messages.size())
                .stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .toList();
    }

    @Transactional
    public String createConversation(String userId, String firstMessage) {
        String convId = generateId();
        LocalDateTime now = LocalDateTime.now();

        ChatConversation conv = new ChatConversation();
        conv.setId(convId);
        conv.setUserId(userId);
        conv.setTitle(firstMessage.length() > 100 ? firstMessage.substring(0, 100) : firstMessage);
        conv.setCreatedAt(now);
        conv.setUpdatedAt(now);
        conversationRepository.save(conv);

        return convId;
    }

    @Transactional
    public void saveUserMessage(String conversationId, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setId(generateId());
        msg.setConversationId(conversationId);
        msg.setRole("user");
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        messageRepository.save(msg);

        bumpUpdatedAt(conversationId);
    }

    @Transactional
    public void saveAssistantMessage(String conversationId, String content,
                                      String plotlyJson, String generatedSql, String intent) {
        ChatMessage msg = new ChatMessage();
        msg.setId(generateId());
        msg.setConversationId(conversationId);
        msg.setRole("assistant");
        msg.setContent(content);
        msg.setPlotlyJson(plotlyJson);
        msg.setGeneratedSql(generatedSql);
        msg.setIntent(intent);
        msg.setCreatedAt(LocalDateTime.now());
        messageRepository.save(msg);

        bumpUpdatedAt(conversationId);
    }

    @Transactional
    public void deleteConversation(String conversationId, Authentication auth) {
        String userId = getUserId(auth);
        ChatConversation conv = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation", conversationId));
        conversationRepository.delete(conv);
    }

    public ConversationResponse updateTitle(String conversationId, String title, Authentication auth) {
        String userId = getUserId(auth);
        ChatConversation conv = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation", conversationId));
        conv.setTitle(title);
        return ConversationResponse.from(conversationRepository.save(conv));
    }

    private void bumpUpdatedAt(String conversationId) {
        conversationRepository.findById(conversationId).ifPresent(c -> {
            c.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(c);
        });
    }
}
