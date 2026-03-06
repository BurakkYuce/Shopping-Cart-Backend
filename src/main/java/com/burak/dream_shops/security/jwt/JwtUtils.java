package com.burak.dream_shops.security.jwt;

import com.burak.dream_shops.security.user.ShopUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

/**
 * JWT (JSON Web Token) işlemlerini yöneten yardımcı sınıf.
 * Token üretme, doğrulama ve içinden kullanıcı adı çıkarma işlemlerini gerçekleştirir.
 * jjwt kütüphanesi kullanılır.
 */
@Component
public class JwtUtils {

    /** application.properties'teki Base64 kodlu gizli anahtar */
    @Value("${auth.token.jwtSecret}")
    private String jwtSecret;

    /** Token geçerlilik süresi (milisaniye cinsinden, örn: 3600000 = 1 saat) */
    @Value("${auth.token.expirationInMils}")
    private int jwtExpirationMs;

    /**
     * Başarılı login sonrası Authentication nesnesinden JWT token üretir.
     * Token içine: email (subject), kullanıcı ID'si ve roller (claims) eklenir.
     * Token, gizli anahtar ile HMAC-SHA ile imzalanır.
     */
    public String generateTokenForUser(Authentication authentication) {
        ShopUserDetails userPrincipal = (ShopUserDetails) authentication.getPrincipal();
        List<String> roles = userPrincipal.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return Jwts.builder()
                .subject(userPrincipal.getEmail())       // token'ın sahibi
                .claim("id", userPrincipal.getId())      // kullanıcı ID'si
                .claim("roles", roles)                    // roller
                .issuedAt(new Date())                     // oluşturulma zamanı
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs)) // bitiş zamanı
                .signWith(key())                          // imzalama
                .compact();
    }

    /**
     * application.properties'teki Base64 secret'ı çözüp HMAC-SHA SecretKey döner.
     * Token imzalama ve doğrulamada kullanılır.
     */
    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    /**
     * Token'ı parse edip içindeki email adresini (subject) döner.
     * AuthTokenFilter'da her request'te hangi kullanıcı olduğunu anlamak için kullanılır.
     */
    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Token'ın geçerli olup olmadığını doğrular.
     * Süresi dolmuş, hatalı biçimlendirilmiş veya imzası yanlış tokenlar için false döner.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException |
                 SecurityException | IllegalArgumentException e) {
            return false;
        }
    }
}
