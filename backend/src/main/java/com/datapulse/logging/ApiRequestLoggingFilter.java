package com.datapulse.logging;

import com.datapulse.security.UserDetailsImpl;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ApiRequestLoggingFilter implements Filter {

    private final LogEventPublisher logEventPublisher;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();
        if (shouldSkip(path)) {
            chain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        long startTime = System.currentTimeMillis();

        try {
            chain.doFilter(wrappedRequest, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            String userId = null;
            String userRole = null;
            String username = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl ud) {
                userId = ud.getId();
                userRole = ud.getRole().name();
                username = ud.getUsername();
            }

            LogEvent.RequestInfo requestInfo = new LogEvent.RequestInfo();
            requestInfo.setMethod(request.getMethod());
            requestInfo.setEndpoint(path);
            requestInfo.setIp(clientIp(request));
            requestInfo.setResponseTimeMs(duration);
            requestInfo.setStatusCode(response.getStatus());

            Map<String, Object> details = new HashMap<>();
            details.put("query", request.getQueryString() != null ? request.getQueryString() : "");
            if (username != null) details.put("username", username);
            String ua = request.getHeader("User-Agent");
            if (ua != null) details.put("user_agent", ua);

            logEventPublisher.publish(LogEventType.API_REQUEST, userId, userRole, details, requestInfo);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }

    private boolean shouldSkip(String path) {
        return path.startsWith("/actuator") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/api-docs");
    }
}
