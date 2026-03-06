package com.burak.dream_shops.security.user;

import com.burak.dream_shops.model.User;
import com.burak.dream_shops.repository.User.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security'nin UserDetailsService arayüzünü implement eden servis.
 * Login sırasında Spring Security bu servisi çağırarak kullanıcıyı DB'den yükler.
 * Email (username olarak) ile kullanıcı aranır.
 */
@Service
@RequiredArgsConstructor
public class ShopUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    /**
     * Verilen email adresine sahip kullanıcıyı DB'den yükler.
     * Bulunan User → ShopUserDetails'e dönüştürülür ve Spring Security'ye verilir.
     * Kullanıcı bulunamazsa UsernameNotFoundException fırlatır (Spring Security bunu handle eder).
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        return ShopUserDetails.buildUserDetails(user);
    }
}
