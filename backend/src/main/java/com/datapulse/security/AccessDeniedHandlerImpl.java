package com.datapulse.security;

import com.datapulse.logging.LogEvent;
import com.datapulse.logging.LogEventPublisher;
import com.datapulse.logging.LogEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LogEventPublisher logEventPublisher;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        publishAccessDenied(request, accessDeniedException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "timestamp", Instant.now().toString(),
                "status", 403,
                "error", "Forbidden",
                "message", "Access denied",
                "path", request.getServletPath()
        ));
    }

    private void publishAccessDenied(HttpServletRequest request, String reason) {
        String userId = null;
        String userRole = null;
        String username = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl ud) {
            userId = ud.getId();
            userRole = ud.getRole().name();
            username = ud.getUsername();
        }

        Map<String, Object> details = new HashMap<>();
        details.put("attack_type", "access_denied");
        if (reason != null) details.put("reason", reason);
        if (username != null) details.put("username", username);
        String ua = request.getHeader("User-Agent");
        if (ua != null) details.put("user_agent", ua);

        LogEvent.RequestInfo requestInfo = new LogEvent.RequestInfo();
        requestInfo.setMethod(request.getMethod());
        requestInfo.setEndpoint(request.getRequestURI());
        requestInfo.setIp(clientIp(request));
        requestInfo.setStatusCode(403);

        logEventPublisher.publish(LogEventType.SECURITY_ACCESS_DENIED, userId, userRole, details, requestInfo);
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }
}
