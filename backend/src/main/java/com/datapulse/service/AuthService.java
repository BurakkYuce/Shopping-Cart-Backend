package com.datapulse.service;

import com.datapulse.dto.request.LoginRequest;
import com.datapulse.dto.request.RegisterRequest;
import com.datapulse.dto.response.AuthResponse;
import com.datapulse.exception.DuplicateEmailException;
import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.exception.UnauthorizedAccessException;
import com.datapulse.logging.LogEventPublisher;
import com.datapulse.logging.LogEventType;
import com.datapulse.model.User;
import com.datapulse.model.enums.AccountStatus;
import com.datapulse.repository.UserRepository;
import com.datapulse.security.JwtUtil;
import com.datapulse.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final LogEventPublisher logEventPublisher;
    private final EmailVerificationTokenService emailVerificationTokenService;
    private final MailService mailService;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    @Value("${app.auth.email-verification.required:true}")
    private boolean emailVerificationRequired;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("User", request.getEmail()));

        if (user.getAccountStatus() != null && user.getAccountStatus() != AccountStatus.ACTIVE) {
            logEventPublisher.publish(
                    LogEventType.AUTH_FAILED,
                    user.getId(),
                    user.getRoleType().name(),
                    Map.of("email", user.getEmail(), "reason", "account_" + user.getAccountStatus().name().toLowerCase())
            );
            throw new UnauthorizedAccessException("Account is " + user.getAccountStatus().name().toLowerCase());
        }

        if (emailVerificationRequired && !user.isEmailVerified()) {
            logEventPublisher.publish(
                    LogEventType.AUTH_FAILED,
                    user.getId(),
                    user.getRoleType().name(),
                    Map.of("email", user.getEmail(), "reason", "email_not_verified")
            );
            throw new UnauthorizedAccessException("Email not verified — please check your inbox for the verification link");
        }

        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        logEventPublisher.publish(
                LogEventType.USER_LOGIN,
                user.getId(),
                user.getRoleType().name(),
                Map.of("email", user.getEmail())
        );

        return new AuthResponse(accessToken, refreshToken, jwtExpiration / 1000, user.getRoleType().name(), user.getId());
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        String userId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        com.datapulse.model.RoleType requestedRole = request.getRoleType();
        com.datapulse.model.RoleType safeRole = requestedRole;
        if (requestedRole == com.datapulse.model.RoleType.ADMIN) {
            logEventPublisher.publish(
                    LogEventType.SECURITY_PRIVILEGE_ESCALATION,
                    null,
                    null,
                    Map.of(
                            "email", request.getEmail(),
                            "attack_type", "privilege_escalation",
                            "reason", "register_body_role_admin",
                            "attack_payload", "roleType=ADMIN"
                    )
            );
            safeRole = com.datapulse.model.RoleType.INDIVIDUAL;
        }

        User user = new User();
        user.setId(userId);
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRoleType(safeRole);
        user.setGender(request.getGender());
        user.setEmailVerified(false);
        userRepository.save(user);

        String token = emailVerificationTokenService.generateAndAssign(user);
        try {
            mailService.sendVerificationEmail(user.getEmail(), token);
            logEventPublisher.publish(
                    LogEventType.EMAIL_VERIFICATION_SENT,
                    userId,
                    user.getRoleType().name(),
                    Map.of("email", user.getEmail())
            );
        } catch (Exception e) {
            logEventPublisher.publish(
                    LogEventType.ERROR_OCCURRED,
                    userId,
                    user.getRoleType().name(),
                    Map.of("email", user.getEmail(), "stage", "verification_email", "error", e.getMessage())
            );
        }

        logEventPublisher.publish(
                LogEventType.USER_REGISTER,
                userId,
                user.getRoleType().name(),
                Map.of("email", user.getEmail())
        );

        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        return new AuthResponse(accessToken, refreshToken, jwtExpiration / 1000, user.getRoleType().name(), userId);
    }

    public void verifyEmail(String token) {
        emailVerificationTokenService.verifyToken(token);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw new UnauthorizedAccessException("Invalid or expired refresh token");
        }

        String email = jwtUtil.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User", email));

        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        String newAccessToken = jwtUtil.generateAccessToken(userDetails);

        return new AuthResponse(newAccessToken, refreshToken, jwtExpiration / 1000, user.getRoleType().name(), user.getId());
    }
}
