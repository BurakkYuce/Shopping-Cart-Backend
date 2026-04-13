package com.datapulse.service;

import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.exception.UnauthorizedAccessException;
import com.datapulse.logging.LogEventPublisher;
import com.datapulse.logging.LogEventType;
import com.datapulse.model.User;
import com.datapulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailVerificationTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final int TOKEN_TTL_HOURS = 24;

    private final UserRepository userRepository;
    private final LogEventPublisher logEventPublisher;
    private final SecureRandom random = new SecureRandom();

    public String generateAndAssign(User user) {
        byte[] buf = new byte[TOKEN_BYTES];
        random.nextBytes(buf);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        user.setEmailVerificationToken(token);
        user.setEmailVerificationExpiresAt(LocalDateTime.now().plusHours(TOKEN_TTL_HOURS));
        user.setEmailVerified(false);
        userRepository.save(user);
        return token;
    }

    @Transactional
    public User verifyToken(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedAccessException("Verification token is required");
        }
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new EntityNotFoundException("VerificationToken", token));

        if (user.getEmailVerificationExpiresAt() == null
                || user.getEmailVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedAccessException("Verification token has expired");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiresAt(null);
        User saved = userRepository.save(user);

        logEventPublisher.publish(
                LogEventType.EMAIL_VERIFIED,
                user.getId(),
                user.getRoleType().name(),
                Map.of("email", user.getEmail()));

        return saved;
    }
}
