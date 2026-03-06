package com.burak.dream_shops.security.user;

import com.burak.dream_shops.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
/**
 * Spring Security'nin kullandığı UserDetails arayüzünü implement eden sınıf.
 * Kimlik doğrulama sırasında kullanıcı bilgilerini taşır.
 * Doğrudan User entity kullanmak yerine bu wrapper class kullanılır;
 * böylece security katmanı ile JPA katmanı birbirinden ayrılmış olur.
 */
public class ShopUserDetails implements UserDetails {
    private Long id;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;

    /**
     * User entity'sinden ShopUserDetails nesnesi oluşturur (factory method).
     * Kullanıcının rollerini (Role) → GrantedAuthority listesine dönüştürür.
     * Spring Security bu authority listesini @PreAuthorize kontrollerinde kullanır.
     */
    public static ShopUserDetails buildUserDetails(User user) {
        List<GrantedAuthority> authorities = user.getRoles()
                .stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
        return new ShopUserDetails(user.getId(), user.getEmail(), user.getPassword(), authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
