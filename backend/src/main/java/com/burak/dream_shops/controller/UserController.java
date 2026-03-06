package com.burak.dream_shops.controller;

import com.burak.dream_shops.dto.UserDto;
import com.burak.dream_shops.exceptions.AlreadyExistsException;
import com.burak.dream_shops.exceptions.ResourcesNotFoundException;
import com.burak.dream_shops.model.User;
import com.burak.dream_shops.request.ApiResponse;
import com.burak.dream_shops.request.CreateUserRequest.CreateUserRequest;
import com.burak.dream_shops.request.UpdateUserRequest.UpdateUserRequest;
import com.burak.dream_shops.service.user.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/users")
public class UserController {
    private final IUserService userService;


    @GetMapping("/{userId}/user")
    public ResponseEntity<ApiResponse> getUserById(@PathVariable Long userId) {
        try {
            UserDto userDto = userService.getUserById(userId);
            return ResponseEntity.ok(new ApiResponse("Success", userDto));
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse> createUser(@RequestBody CreateUserRequest request) {
        try {
            UserDto userDto = userService.createUser(request);
            return ResponseEntity.ok(new ApiResponse("Create User Success!", userDto));
        } catch (AlreadyExistsException e) {
            return ResponseEntity.status(CONFLICT).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or #userId == authentication.principal.id")
    @PutMapping("/{userId}/update")
    public ResponseEntity<ApiResponse> updateUser(@RequestBody UpdateUserRequest request, @PathVariable Long userId) {
        try {
            UserDto userDto = userService.updateUser(request, userId);
            return ResponseEntity.ok(new ApiResponse("Update User Success!", userDto));
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }
    @PreAuthorize("hasRole('ROLE_ADMIN') or #userId == authentication.principal.id")
    @DeleteMapping("/{userId}/delete")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long userId) {
        try {
            userService.deleteUser(userId);
            return ResponseEntity.ok(new ApiResponse("Delete User Success!", null));
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }
}