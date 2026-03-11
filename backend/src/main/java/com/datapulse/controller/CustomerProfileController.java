package com.datapulse.controller;

import com.datapulse.model.CustomerProfile;
import com.datapulse.service.CustomerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer-profiles")
@RequiredArgsConstructor
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(Authentication auth) {
        return ResponseEntity.ok(customerProfileService.getMyProfile(auth));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(
            Authentication auth,
            @RequestBody CustomerProfile body) {
        return ResponseEntity.ok(customerProfileService.updateMyProfile(auth, body));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProfileById(@PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(customerProfileService.getProfileById(id, auth));
    }
}
