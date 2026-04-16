package com.datapulse.controller;

import com.datapulse.dto.request.UpdateNotificationPreferenceRequest;
import com.datapulse.dto.response.NotificationPreferenceResponse;
import com.datapulse.security.UserDetailsImpl;
import com.datapulse.service.NotificationPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification-preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final NotificationPreferenceService service;

    @GetMapping("/me")
    public ResponseEntity<NotificationPreferenceResponse> getMe(Authentication auth) {
        String userId = ((UserDetailsImpl) auth.getPrincipal()).getId();
        return ResponseEntity.ok(NotificationPreferenceResponse.from(service.getOrCreate(userId)));
    }

    @PutMapping("/me")
    public ResponseEntity<NotificationPreferenceResponse> updateMe(
            Authentication auth,
            @RequestBody UpdateNotificationPreferenceRequest req) {
        return ResponseEntity.ok(NotificationPreferenceResponse.from(service.update(auth, req)));
    }
}
