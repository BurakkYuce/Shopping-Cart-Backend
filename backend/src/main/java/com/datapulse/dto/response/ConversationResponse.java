package com.datapulse.dto.response;

import com.datapulse.model.ChatConversation;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationResponse {
    private String id;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ConversationResponse from(ChatConversation c) {
        ConversationResponse r = new ConversationResponse();
        r.id = c.getId();
        r.title = c.getTitle();
        r.createdAt = c.getCreatedAt();
        r.updatedAt = c.getUpdatedAt();
        return r;
    }
}
