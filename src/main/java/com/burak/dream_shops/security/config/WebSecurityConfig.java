package com.burak.dream_shops.security.config;

import com.burak.dream_shops.security.jwt.AuthEntryPointJwt;
import com.burak.dream_shops.security.jwt.AuthTokenFilter;
import com.burak.dream_shops.security.user.ShopUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security yapılandırma sınıfı.
 *
 * Temel görevleri:
 * - Hangi endpoint'lerin public, hangilerinin korumalı olduğunu belirler
 * - JWT tabanlı stateless session yönetimi kurar
 * - Şifre encoder, authentication provider ve manager bean'lerini tanımlar
 * - JWT filter'ı Spring Security filter chain'e ekler
 *
 * @EnableMethodSecurity → controller metotlarında @PreAuthorize kullanımını aktif eder
 */
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Configuration
public class WebSecurityConfig {
    private final ShopUserDetailsService userDetailsService;
    private final AuthEntryPointJwt authEntryPointJwt;
    private final AuthTokenFilter authTokenFilter;

    /**
     * Şifreleri BCrypt algoritması ile hash'ler.
     * Kayıt ve login sırasında şifre karşılaştırmak için kullanılır.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Security'nin AuthenticationManager'ını expose eder.
     * AuthController'da login işlemi için manuel olarak kullanılır.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Kullanıcıyı DB'den yükleyen ve şifre doğrulayan authentication provider.
     * ShopUserDetailsService (kullanıcıyı yükler) + BCryptPasswordEncoder (şifreyi doğrular) kullanır.
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * HTTP güvenlik yapılandırmasını tanımlar.
     *
     * Kurallar:
     * - CSRF devre dışı (REST API, cookie kullanmıyor)
     * - Session STATELESS: her request kendi JWT'sini taşır, sunucuda oturum tutulmaz
     * - /api/v1/auth/** ve /api/v1/users/add herkese açık (login ve kayıt)
     * - GET /api/v1/products/**, GET /api/v1/categories/** herkese açık (ürün/kategori listeleme)
     * - Diğer tüm istekler için JWT zorunlu
     * - AuthTokenFilter, UsernamePasswordAuthenticationFilter'dan önce çalışır
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception.authenticationEntryPoint(authEntryPointJwt))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/users/add").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/api/v1/products/**", "/api/v1/categories/**").permitAll()
                        .anyRequest().authenticated()
                );
        http.authenticationProvider(daoAuthenticationProvider());
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
