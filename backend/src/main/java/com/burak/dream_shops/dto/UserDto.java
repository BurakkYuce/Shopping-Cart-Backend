package com.burak.dream_shops.dto;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class UserDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Set<String> roles;
    private List<OrderDto> orders;
    private CartDto cart;
}
