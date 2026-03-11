package com.datapulse.service;

import com.datapulse.dto.request.LoginRequest;
import com.datapulse.dto.request.RegisterRequest;
import com.datapulse.dto.response.AuthResponse;
import com.datapulse.exception.DuplicateEmailException;
import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.logging.LogEventPublisher;
import com.datapulse.model.RoleType;
import com.datapulse.model.User;
import com.datapulse.repository.CustomerProfileRepository;
import com.datapulse.repository.UserRepository;
import com.datapulse.security.JwtUtil;
import com.datapulse.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpiration", 86400000L);
    }

    @Test
    void login_success() {
        User user = new User("user1", "test@example.com", "hashed-password", RoleType.INDIVIDUAL, "male");

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(any(UserDetailsImpl.class)))
                .thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(UserDetailsImpl.class)))
                .thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
    }

    @Test
    void login_userNotFound_throwsEntityNotFoundException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@example.com");
        request.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> authService.login(request));
    }

    @Test
    void register_success() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setPassword("securepass");
        request.setRoleType(RoleType.INDIVIDUAL);
        request.setGender("female");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("securepass")).thenReturn("hashed");
        when(jwtUtil.generateAccessToken(any(UserDetailsImpl.class))).thenReturn("tok");
        when(jwtUtil.generateRefreshToken(any(UserDetailsImpl.class))).thenReturn("refresh-tok");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("INDIVIDUAL", response.getUserRole());
        assertEquals("tok", response.getAccessToken());
        verify(userRepository, times(1)).save(any(com.datapulse.model.User.class));
        verify(customerProfileRepository, times(1)).save(any(com.datapulse.model.CustomerProfile.class));
    }

    @Test
    void register_duplicateEmail_throwsDuplicateEmailException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("pass");
        request.setRoleType(RoleType.INDIVIDUAL);

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.register(request));
    }
}
