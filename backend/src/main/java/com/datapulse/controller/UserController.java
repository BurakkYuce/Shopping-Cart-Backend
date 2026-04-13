package com.datapulse.controller;

import com.datapulse.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication auth) {
        return ResponseEntity.ok(userService.getCurrentUser(auth));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(userService.getUserById(id, auth));
    }

    @PatchMapping("/me")
    public ResponseEntity<?> updateCurrentUser(
            Authentication auth,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(userService.updateCurrentUser(auth, body));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable String id,
            Authentication auth,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(userService.updateUser(id, auth, body));
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers(Authentication auth) {
        return ResponseEntity.ok(userService.getAllUsers(auth));
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(
            Authentication auth,
            @RequestBody Map<String, String> body) {
        userService.changePassword(auth, body.get("currentPassword"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }
}
