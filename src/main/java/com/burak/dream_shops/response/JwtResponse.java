package com.burak.dream_shops.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwtResponse {
    private Long id;
    private String token;
    private String tokenType = "Bearer";

    public JwtResponse(Long id, String token) {
        this.id = id;
        this.token = token;
    }
}
