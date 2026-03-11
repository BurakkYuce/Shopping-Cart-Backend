package com.datapulse.service;

import com.datapulse.dto.request.LoginRequest;
import com.datapulse.dto.request.RegisterRequest;
import com.datapulse.dto.response.AuthResponse;
import com.datapulse.exception.DuplicateEmailException;
import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.exception.UnauthorizedAccessException;
import com.datapulse.logging.LogEventPublisher;
import com.datapulse.logging.LogEventType;
import com.datapulse.model.CustomerProfile;
import com.datapulse.model.User;
import com.datapulse.repository.CustomerProfileRepository;
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
    private final CustomerProfileRepository customerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final LogEventPublisher logEventPublisher;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("User", request.getEmail()));

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

        User user = new User();
        user.setId(userId);
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRoleType(request.getRoleType());
        user.setGender(request.getGender());
        userRepository.save(user);

        String profileId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        CustomerProfile profile = new CustomerProfile();
        profile.setId(profileId);
        profile.setUserId(userId);
        customerProfileRepository.save(profile);

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
