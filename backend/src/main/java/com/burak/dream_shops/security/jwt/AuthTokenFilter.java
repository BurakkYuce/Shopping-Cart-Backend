package com.burak.dream_shops.security.jwt;

import com.burak.dream_shops.security.user.ShopUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Her HTTP isteğinde bir kez çalışan JWT doğrulama filtresi.
 * OncePerRequestFilter'dan türer: aynı request için birden fazla çalışmaz.
 *
 * Akış:
 * 1. Request'in Authorization header'ından "Bearer <token>" kısmını al
 * 2. Token geçerliyse kullanıcıyı DB'den yükle
 * 3. Authentication nesnesini SecurityContext'e set et
 * 4. İsteği bir sonraki filtreye ilet
 */
@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final ShopUserDetailsService userDetailsService;

    /**
     * Her request için çalışır.
     * Token geçerliyse kullanıcıyı authenticate eder ve SecurityContextHolder'a yazar.
     * Token yoksa veya geçersizse hiçbir şey yapmaz; request devam eder (401 alabilir).
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String jwt = parseJwt(request);
        if (jwt != null && jwtUtils.validateToken(jwt)) {
            // Token'dan email çek, DB'den kullanıcıyı yükle
            String username = jwtUtils.getUsernameFromToken(jwt);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            // Authentication nesnesi oluştur ve SecurityContext'e koy
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response); // bir sonraki filtreye geç
    }

    /**
     * Request header'ından JWT token'ı çıkarır.
     * "Authorization: Bearer eyJ..." formatını bekler.
     * "Bearer " prefix'ini kaldırıp sadece token string'ini döner.
     * Header yoksa veya format yanlışsa null döner.
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7); // "Bearer " (7 karakter) sonrasını al
        }
        return null;
    }
}
