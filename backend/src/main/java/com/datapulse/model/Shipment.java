package com.datapulse.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Order order;

    private String warehouse;

    private String mode;

    private String status;

    @Column(name = "customer_care_calls")
    private Integer customerCareCalls;

    @Column(name = "customer_rating")
    private Integer customerRating;

    @Column(name = "weight_gms")
    private Integer weightGms;

    @Column(name = "warehouse_block", length = 10)
    private String warehouseBlock;

    @Column(name = "cost")
    private Double cost;

    @Column(name = "prior_purchases")
    private Integer priorPurchases;

    @Column(name = "importance", length = 20)
    private String importance;

    @Column(name = "discount_offered")
    private Double discountOffered;
}
