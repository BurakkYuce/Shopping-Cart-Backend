package com.burak.dream_shops.service.user;

import com.burak.dream_shops.dto.UserDto;
import com.burak.dream_shops.model.User;
import com.burak.dream_shops.request.CreateUserRequest.CreateUserRequest;
import com.burak.dream_shops.request.UpdateUserRequest.UpdateUserRequest;

public interface IUserService {

    UserDto getUserById(Long userId);
    UserDto createUser(CreateUserRequest request);
    User getUserEntityById(Long userId);  // ← ekle
    UserDto updateUser(UpdateUserRequest request, Long userId);
    void deleteUser(Long userId);
    UserDto convertUserToDto(User user);
}