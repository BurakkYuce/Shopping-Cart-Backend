package com.datapulse.security;

import com.datapulse.logging.LogEvent;
import com.datapulse.logging.LogEventPublisher;
import com.datapulse.logging.LogEventType;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final LogEventPublisher logEventPublisher;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();
        if (path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String email;
        try {
            email = jwtUtil.extractEmail(token);
            if (jwtUtil.isTokenExpired(token)) {
                publishInvalidToken(request, "expired", null);
                filterChain.doFilter(request, response);
                return;
            }
        } catch (ExpiredJwtException e) {
            publishInvalidToken(request, "expired", e.getClaims() != null ? e.getClaims().getSubject() : null);
            filterChain.doFilter(request, response);
            return;
        } catch (SignatureException e) {
            publishInvalidToken(request, "signature", null);
            filterChain.doFilter(request, response);
            return;
        } catch (MalformedJwtException e) {
            publishInvalidToken(request, "malformed", null);
            filterChain.doFilter(request, response);
            return;
        } catch (UnsupportedJwtException e) {
            publishInvalidToken(request, "unsupported", null);
            filterChain.doFilter(request, response);
            return;
        } catch (JwtException | IllegalArgumentException e) {
            publishInvalidToken(request, "invalid", null);
            filterChain.doFilter(request, response);
            return;
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(email);
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails, token, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }

    private void publishInvalidToken(HttpServletRequest request, String reason, String subject) {
        Map<String, Object> details = new HashMap<>();
        details.put("reason", reason);
        details.put("attack_type", "invalid_token");
        if (subject != null) details.put("username", subject);
        String ua = request.getHeader("User-Agent");
        if (ua != null) details.put("user_agent", ua);

        LogEvent.RequestInfo requestInfo = new LogEvent.RequestInfo();
        requestInfo.setMethod(request.getMethod());
        requestInfo.setEndpoint(request.getRequestURI());
        requestInfo.setIp(clientIp(request));
        requestInfo.setStatusCode(401);

        logEventPublisher.publish(LogEventType.SECURITY_INVALID_TOKEN, null, null, details, requestInfo);
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
