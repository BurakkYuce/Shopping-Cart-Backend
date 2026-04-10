package com.datapulse.dto.response;

import com.datapulse.model.ChatConversation;
import com.datapulse.model.ChatMessage;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConversationDetailResponse {
    private String id;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MessageResponse> messages;

    @Data
    public static class MessageResponse {
        private String id;
        private String role;
        private String content;
        private String plotlyJson;
        private String generatedSql;
        private String intent;
        private LocalDateTime createdAt;

        public static MessageResponse from(ChatMessage m) {
            MessageResponse r = new MessageResponse();
            r.id = m.getId();
            r.role = m.getRole();
            r.content = m.getContent();
            r.plotlyJson = m.getPlotlyJson();
            r.generatedSql = m.getGeneratedSql();
            r.intent = m.getIntent();
            r.createdAt = m.getCreatedAt();
            return r;
        }
    }

    public static ConversationDetailResponse from(ChatConversation c, List<ChatMessage> messages) {
        ConversationDetailResponse r = new ConversationDetailResponse();
        r.id = c.getId();
        r.title = c.getTitle();
        r.createdAt = c.getCreatedAt();
        r.updatedAt = c.getUpdatedAt();
        r.messages = messages.stream().map(MessageResponse::from).toList();
        return r;
    }
}
