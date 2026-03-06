package com.burak.dream_shops.data;

import com.burak.dream_shops.model.Role;
import com.burak.dream_shops.model.User;
import com.burak.dream_shops.repository.RoleRepository.RoleRepository;
import com.burak.dream_shops.repository.User.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationListener<ApplicationReadyEvent> {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        createDefaultRolesIfNotExists();
        createDefaultUsersIfNotExists();
        createDefaultAdminIfNotExists();
    }

    private void createDefaultRolesIfNotExists() {
        for (String roleName : new String[]{"ROLE_USER", "ROLE_ADMIN"}) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(new Role(roleName));
            }
        }
    }

    private void createDefaultUsersIfNotExists() {
        Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow();
        for (int i = 0; i <= 5; i++) {
            final int index = i;
            String defaultEmail = "user" + index + "@email.com";
            userRepository.findByEmail(defaultEmail).ifPresentOrElse(user -> {
                if (!user.getPassword().startsWith("$2")) {
                    user.setPassword(passwordEncoder.encode("123456"));
                    if (user.getRoles() == null || user.getRoles().isEmpty()) {
                        user.setRoles(Set.of(userRole));
                    }
                    userRepository.save(user);
                    System.out.println("Updated plain-text password for: " + user.getEmail());
                }
            }, () -> {
                User user = new User();
                user.setLastName("lastname" + index);
                user.setFirstName("firstname" + index);
                user.setEmail(defaultEmail);
                user.setPassword(passwordEncoder.encode("123456"));
                user.setRoles(Set.of(userRole));
                userRepository.save(user);
                System.out.println("Default user" + index + " created.");
            });
        }
    }

    private void createDefaultAdminIfNotExists() {
        String adminEmail = "admin@email.com";
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
        User admin = new User();
        admin.setFirstName("Admin");
        admin.setLastName("Admin");
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRoles(Set.of(adminRole));
        userRepository.save(admin);
        System.out.println("Default admin created.");
    }
}
