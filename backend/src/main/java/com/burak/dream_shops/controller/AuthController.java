package com.burak.dream_shops.controller;

import com.burak.dream_shops.request.ApiResponse;
import com.burak.dream_shops.request.LoginRequest;
import com.burak.dream_shops.response.JwtResponse;
import com.burak.dream_shops.security.jwt.JwtUtils;
import com.burak.dream_shops.security.user.ShopUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Kimlik doğrulama (authentication) işlemlerini yöneten controller.
 * Login endpoint'i burada tanımlanır.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    /**
     * Kullanıcı girişi yapar ve JWT token döner.
     *
     * POST /api/v1/auth/login
     * Body: { "email": "...", "password": "..." }
     *
     * Akış:
     * 1. Email + şifre ile authenticate et (DaoAuthenticationProvider devreye girer)
     * 2. Başarılıysa Authentication'ı SecurityContext'e yaz
     * 3. JWT token üret ve JwtResponse içinde döndür
     * 4. Hatalı şifrede Spring Security otomatik 401 fırlatır
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateTokenForUser(authentication);
        ShopUserDetails userDetails = (ShopUserDetails) authentication.getPrincipal();
        JwtResponse jwtResponse = new JwtResponse(userDetails.getId(), jwt);
        return ResponseEntity.ok(new ApiResponse("Login successful!", jwtResponse));
    }
}
