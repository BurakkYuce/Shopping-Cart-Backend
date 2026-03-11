package com.datapulse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private String userRole;
    private String userId;

    public AuthResponse(String accessToken, String refreshToken, long expiresIn, String userRole, String userId) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.userRole = userRole;
        this.userId = userId;
    }
}
