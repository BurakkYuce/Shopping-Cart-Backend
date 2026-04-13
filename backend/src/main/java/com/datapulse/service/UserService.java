package com.datapulse.service;

import com.datapulse.dto.response.UserResponse;
import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.exception.UnauthorizedAccessException;
import com.datapulse.model.RoleType;
import com.datapulse.model.User;
import com.datapulse.repository.UserRepository;
import com.datapulse.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private UserDetailsImpl extractCurrentUser(Authentication auth) {
        return (UserDetailsImpl) auth.getPrincipal();
    }

    public UserResponse getCurrentUser(Authentication auth) {
        UserDetailsImpl userDetails = extractCurrentUser(auth);
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new EntityNotFoundException("User", userDetails.getId()));
        return UserResponse.from(user);
    }

    public UserResponse getUserById(String id, Authentication auth) {
        UserDetailsImpl currentUser = extractCurrentUser(auth);
        boolean isAdmin = currentUser.getRole() == RoleType.ADMIN;
        boolean isSelf = currentUser.getId().equals(id);

        if (!isAdmin && !isSelf) {
            throw new UnauthorizedAccessException("Access denied: you can only view your own profile");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));
        return UserResponse.from(user);
    }

    public List<UserResponse> getAllUsers(Authentication auth) {
        UserDetailsImpl currentUser = extractCurrentUser(auth);
        if (currentUser.getRole() != RoleType.ADMIN) {
            throw new UnauthorizedAccessException("Access denied: ADMIN role required");
        }
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse updateCurrentUser(Authentication auth, Map<String, Object> updates) {
        UserDetailsImpl currentUser = extractCurrentUser(auth);
        return updateUser(currentUser.getId(), auth, updates);
    }

    public UserResponse updateUser(String id, Authentication auth, Map<String, Object> updates) {
        UserDetailsImpl currentUser = extractCurrentUser(auth);
        boolean isAdmin = currentUser.getRole() == RoleType.ADMIN;
        boolean isSelf = currentUser.getId().equals(id);

        if (!isAdmin && !isSelf) {
            throw new UnauthorizedAccessException("Access denied: you can only update your own profile");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));

        if (updates.containsKey("email")) {
            user.setEmail((String) updates.get("email"));
        }
        if (updates.containsKey("gender")) {
            user.setGender((String) updates.get("gender"));
        }

        userRepository.save(user);
        return UserResponse.from(user);
    }

    public void changePassword(Authentication auth, String currentPassword, String newPassword) {
        UserDetailsImpl current = extractCurrentUser(auth);
        User user = userRepository.findById(current.getId())
                .orElseThrow(() -> new EntityNotFoundException("User", current.getId()));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new UnauthorizedAccessException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
