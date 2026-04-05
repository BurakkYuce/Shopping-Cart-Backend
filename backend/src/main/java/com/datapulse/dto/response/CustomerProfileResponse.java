package com.datapulse.dto.response;

import com.datapulse.model.CustomerProfile;
import lombok.Data;

@Data
public class CustomerProfileResponse {
    private String id;
    private String userId;
    private Integer age;
    private String city;
    private String membershipType;
    private Double totalSpend;
    private Integer itemsPurchased;
    private Double averageRating;
    private String satisfactionLevel;

    public static CustomerProfileResponse from(CustomerProfile profile) {
        CustomerProfileResponse r = new CustomerProfileResponse();
        r.id = profile.getId();
        r.userId = profile.getUserId();
        r.age = profile.getAge();
        r.city = profile.getCity();
        r.membershipType = profile.getMembershipType();
        r.totalSpend = profile.getTotalSpend();
        r.itemsPurchased = profile.getItemsPurchased();
        r.averageRating = profile.getAverageRating();
        r.satisfactionLevel = profile.getSatisfactionLevel();
        return r;
    }
}
