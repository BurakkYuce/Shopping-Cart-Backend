package com.datapulse.controller;

import com.datapulse.exception.UnauthorizedAccessException;
import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.model.PasswordResetToken;
import com.datapulse.model.User;
import com.datapulse.repository.PasswordResetTokenRepository;
import com.datapulse.repository.UserRepository;
import com.datapulse.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class PasswordResetController {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String tokenValue = UUID.randomUUID().toString();

            PasswordResetToken token = new PasswordResetToken();
            token.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 8));
            token.setUserId(user.getId());
            token.setToken(tokenValue);
            token.setExpiresAt(LocalDateTime.now().plusHours(1));
            token.setUsed(false);
            tokenRepository.save(token);

            mailService.sendPasswordResetEmail(email, tokenValue);
        } else {
            log.info("Password reset requested for non-existent email: {}", email);
        }

        return ResponseEntity.ok(Map.of(
                "message", "If an account exists with this email, a reset link has been sent."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String tokenValue = body.get("token");
        String newPassword = body.get("newPassword");

        PasswordResetToken token = tokenRepository.findByTokenAndUsedFalse(tokenValue)
                .orElseThrow(() -> new UnauthorizedAccessException("Invalid or expired reset token"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedAccessException("Reset token has expired");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User", token.getUserId()));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setEmailVerified(true);
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully"));
    }
}
