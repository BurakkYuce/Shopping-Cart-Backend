package com.burak.dream_shops.request.CreateUserRequest;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;

}
