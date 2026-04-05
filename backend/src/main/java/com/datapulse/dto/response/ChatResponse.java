package com.datapulse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponse {
    private String message;
    private String sessionId;
    private String status;
    private String plotlyJson;  // Plotly figure JSON, null if no chart

    public ChatResponse(String message, String sessionId, String status) {
        this.message = message;
        this.sessionId = sessionId;
        this.status = status;
    }
}
