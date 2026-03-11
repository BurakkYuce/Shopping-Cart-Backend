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
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl ud) {
                userId = ud.getId();
                userRole = ud.getRole().name();
            }

            LogEvent.RequestInfo requestInfo = new LogEvent.RequestInfo();
            requestInfo.setMethod(request.getMethod());
            requestInfo.setEndpoint(path);
            requestInfo.setIp(request.getRemoteAddr());
            requestInfo.setResponseTimeMs(duration);
            requestInfo.setStatusCode(response.getStatus());

            logEventPublisher.publish(LogEventType.API_REQUEST, userId, userRole,
                    Map.of("query", request.getQueryString() != null ? request.getQueryString() : ""),
                    requestInfo);
        }
    }

    private boolean shouldSkip(String path) {
        return path.startsWith("/actuator") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/api-docs");
    }
}
