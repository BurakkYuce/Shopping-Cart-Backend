package com.datapulse.dto.response;

import com.datapulse.model.RoleType;
import com.datapulse.model.User;
import lombok.Data;

@Data
public class UserResponse {
    private String id;
    private String email;
    private RoleType roleType;
    private String gender;

    public static UserResponse from(User user) {
        UserResponse r = new UserResponse();
        r.id = user.getId();
        r.email = user.getEmail();
        r.roleType = user.getRoleType();
        r.gender = user.getGender();
        return r;
    }
}
