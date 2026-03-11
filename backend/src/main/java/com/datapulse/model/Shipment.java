package com.datapulse.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "shipments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {

    @Id
    private String id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    private String warehouse;

    private String mode;

    private String status;

    @Column(name = "customer_care_calls")
    private Integer customerCareCalls;

    @Column(name = "customer_rating")
    private Integer customerRating;

    @Column(name = "weight_gms")
    private Integer weightGms;
}
