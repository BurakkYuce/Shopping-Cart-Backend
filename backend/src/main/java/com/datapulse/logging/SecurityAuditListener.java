package com.datapulse.logging;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SecurityAuditListener {

    private final LogEventPublisher logEventPublisher;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        String email = principal instanceof UserDetails ud ? ud.getUsername() : principal.toString();
        logEventPublisher.publish(LogEventType.USER_LOGIN, null, null,
                Map.of("email", email, "message", "Authentication successful"));
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String email = event.getAuthentication().getName();
        logEventPublisher.publish(LogEventType.AUTH_FAILED, null, null,
                Map.of("email", email, "reason", event.getException().getMessage()));
    }
}
