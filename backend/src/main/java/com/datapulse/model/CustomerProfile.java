package com.datapulse.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "customer_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfile {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private User user;

    private Integer age;

    private String city;

    @Column(name = "membership_type")
    private String membershipType;

    @Column(name = "total_spend")
    private Double totalSpend;

    @Column(name = "items_purchased")
    private Integer itemsPurchased;

    @Column(name = "average_rating")
    private Double averageRating;

    @Column(name = "satisfaction_level")
    private String satisfactionLevel;
}
