package com.datapulse.controller;

import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.exception.UnauthorizedAccessException;
import com.datapulse.model.PasswordResetToken;
import com.datapulse.model.User;
import com.datapulse.repository.PasswordResetTokenRepository;
import com.datapulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User", email));

        String tokenValue = UUID.randomUUID().toString();

        PasswordResetToken token = new PasswordResetToken();
        token.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        token.setUserId(user.getId());
        token.setToken(tokenValue);
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setUsed(false);
        tokenRepository.save(token);

        // In production, send this token via email.
        // For now, return it directly (development only).
        return ResponseEntity.ok(Map.of(
                "message", "Password reset token generated. In production, this would be sent via email.",
                "resetToken", tokenValue
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
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully"));
    }
}
