package com.datapulse.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ChatRequest {
    @NotBlank
    private String message;
    private String sessionId;
    private String conversationId;
    private List<Map<String, String>> messageHistory;
}
