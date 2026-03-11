package com.datapulse.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.Markers;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogEventPublisher {

    public void publish(LogEventType eventType, String userId, String userRole, Map<String, Object> details) {
        publish(eventType, userId, userRole, details, null);
    }

    public void publish(LogEventType eventType, String userId, String userRole,
                        Map<String, Object> details, LogEvent.RequestInfo requestInfo) {
        try {
            Map<String, Object> fields = new HashMap<>();
            fields.put("event_type", eventType.name());
            fields.put("timestamp", Instant.now().toString());
            fields.put("service", "ecommerce-backend");
            if (userId != null)   fields.put("user_id", userId);
            if (userRole != null) fields.put("user_role", userRole);
            if (details != null)  fields.put("details", details);

            if (requestInfo != null) {
                Map<String, Object> req = new HashMap<>();
                req.put("method", requestInfo.getMethod());
                req.put("endpoint", requestInfo.getEndpoint());
                req.put("ip", requestInfo.getIp());
                req.put("response_time_ms", requestInfo.getResponseTimeMs());
                req.put("status_code", requestInfo.getStatusCode());
                fields.put("request", req);
            }

            log.info(Markers.appendEntries(fields), eventType.name());
        } catch (Exception e) {
            log.error("Failed to publish log event", e);
        }
    }
}
