package com.datapulse.controller;

import com.datapulse.dto.request.CreateAddressRequest;
import com.datapulse.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<?> getMyAddresses(Authentication auth) {
        return ResponseEntity.ok(addressService.getMyAddresses(auth));
    }

    @PostMapping
    public ResponseEntity<?> createAddress(
            @Valid @RequestBody CreateAddressRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.createAddress(request, auth));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAddress(
            @PathVariable String id,
            @Valid @RequestBody CreateAddressRequest request,
            Authentication auth) {
        return ResponseEntity.ok(addressService.updateAddress(id, request, auth));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable String id, Authentication auth) {
        addressService.deleteAddress(id, auth);
        return ResponseEntity.noContent().build();
    }
}
