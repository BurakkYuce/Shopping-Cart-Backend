package com.datapulse.security;

import com.datapulse.logging.LogEvent;
import com.datapulse.logging.LogEventPublisher;
import com.datapulse.logging.LogEventType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class SecurityAttackDetectionFilter extends OncePerRequestFilter {

    private final LogEventPublisher logEventPublisher;

    private static final Pattern XSS_PATTERN = Pattern.compile(
            "(?i)(<script\\b|javascript:|on(?:error|load|click|mouseover|focus)\\s*=|<iframe\\b|<img[^>]+src\\s*=\\s*[\"']?javascript:|<svg[^>]+on\\w+\\s*=)");

    private static final Pattern SQLI_PATTERN = Pattern.compile(
            "(?i)(\\bunion\\b\\s+\\bselect\\b|\\bor\\b\\s+['\"]?\\d+['\"]?\\s*=\\s*['\"]?\\d+['\"]?|'\\s*or\\s*'1'\\s*=\\s*'1|'\\s*;\\s*drop\\b|\\bexec\\s*\\(|--\\s*$|/\\*.*\\*/)");

    private static final int MAX_BODY_SCAN_BYTES = 64 * 1024;
    private static final int PAYLOAD_SNIPPET_MAX = 300;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (shouldSkip(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrapped = (request instanceof ContentCachingRequestWrapper w)
                ? w
                : new ContentCachingRequestWrapper(request);

        String queryString = wrapped.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            String decoded;
            try {
                decoded = URLDecoder.decode(queryString, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                decoded = queryString;
            }
            scanAndReport(wrapped, decoded, "query");
        }

        try {
            filterChain.doFilter(wrapped, response);
        } finally {
            byte[] body = wrapped.getContentAsByteArray();
            if (body != null && body.length > 0 && isScanableContentType(wrapped.getContentType())) {
                int len = Math.min(body.length, MAX_BODY_SCAN_BYTES);
                String bodyStr = new String(body, 0, len, StandardCharsets.UTF_8);
                scanAndReport(wrapped, bodyStr, "body");
            }
        }
    }

    private void scanAndReport(HttpServletRequest request, String payload, String source) {
        if (payload == null || payload.isEmpty()) return;

        Matcher xss = XSS_PATTERN.matcher(payload);
        if (xss.find()) {
            publish(request, LogEventType.SECURITY_XSS_ATTEMPT, "xss", xss.group(), payload, source);
        }

        Matcher sqli = SQLI_PATTERN.matcher(payload);
        if (sqli.find()) {
            publish(request, LogEventType.SECURITY_SQLI_ATTEMPT, "sqli", sqli.group(), payload, source);
        }
    }

    private void publish(HttpServletRequest request, LogEventType type, String attackType,
                         String pattern, String payload, String source) {
        String userId = null;
        String userRole = null;
        String username = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl ud) {
            userId = ud.getId();
            userRole = ud.getRole().name();
            username = ud.getUsername();
        }

        String snippet = payload.length() > PAYLOAD_SNIPPET_MAX
                ? payload.substring(0, PAYLOAD_SNIPPET_MAX) + "…"
                : payload;

        Map<String, Object> details = new HashMap<>();
        details.put("attack_type", attackType);
        details.put("attack_pattern", pattern);
        details.put("attack_payload", snippet);
        details.put("attack_source", source);
        if (username != null) details.put("username", username);
        String ua = request.getHeader("User-Agent");
        if (ua != null) details.put("user_agent", ua);

        LogEvent.RequestInfo requestInfo = new LogEvent.RequestInfo();
        requestInfo.setMethod(request.getMethod());
        requestInfo.setEndpoint(request.getRequestURI());
        requestInfo.setIp(clientIp(request));

        logEventPublisher.publish(type, userId, userRole, details, requestInfo);
    }

    private boolean shouldSkip(String path) {
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs");
    }

    private boolean isScanableContentType(String contentType) {
        if (contentType == null) return false;
        String ct = contentType.toLowerCase();
        if (ct.startsWith("multipart/")) return false;
        if (ct.startsWith("application/octet-stream")) return false;
        if (ct.startsWith("image/") || ct.startsWith("video/") || ct.startsWith("audio/")) return false;
        return ct.startsWith("application/json")
                || ct.startsWith("application/x-www-form-urlencoded")
                || ct.startsWith("text/");
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
