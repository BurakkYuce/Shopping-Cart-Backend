package com.datapulse.logging;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
public class LogEvent {
    private String timestamp = Instant.now().toString();
    private String level = "INFO";
    private String service = "ecommerce-backend";
    private String eventType;
    private String userId;
    private String userRole;
    private Map<String, Object> details;
    private RequestInfo request;

    @Data
    @NoArgsConstructor
    public static class RequestInfo {
        private String method;
        private String endpoint;
        private String ip;
        private Long responseTimeMs;
        private Integer statusCode;
    }
}
