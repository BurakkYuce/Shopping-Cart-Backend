package com.datapulse.security;

import com.datapulse.model.RoleType;
import com.datapulse.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "ZGV2LXNlY3JldC1rZXktMzJjaGFycy1taW5pbXVtLXJlcXVpcmVk");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 604800000L);

        User user = new User();
        user.setId("user1");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed");
        user.setRoleType(RoleType.INDIVIDUAL);
        user.setGender("male");
        userDetails = new UserDetailsImpl(user);
    }

    @Test
    void generateAndValidateAccessToken() {
        String token = jwtUtil.generateAccessToken(userDetails);

        assertNotNull(token);
        assertTrue(jwtUtil.isTokenValid(token));
        assertEquals("test@example.com", jwtUtil.extractEmail(token));
        assertEquals("INDIVIDUAL", jwtUtil.extractRole(token));
    }

    @Test
    void generateRefreshToken_isValid() {
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        assertNotNull(refreshToken);
        assertTrue(jwtUtil.isTokenValid(refreshToken));
    }

    @Test
    void isTokenExpired_notExpired() {
        String token = jwtUtil.generateAccessToken(userDetails);

        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    void extractUserId_returnsCorrectId() {
        String token = jwtUtil.generateAccessToken(userDetails);

        assertEquals("user1", jwtUtil.extractUserId(token));
    }
}
