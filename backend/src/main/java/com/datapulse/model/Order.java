package com.datapulse.model;

import com.datapulse.model.enums.OrderStatus;
import com.datapulse.model.enums.PaymentMethod;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private User user;

    @Column(name = "store_id", nullable = false)
    private String storeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", insertable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Store store;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "subtotal")
    private Double subtotal;

    @Column(name = "tax_amount")
    private Double taxAmount;

    @Column(name = "grand_total")
    private Double grandTotal;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(length = 50)
    private String carrier;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "return_deadline")
    private LocalDateTime returnDeadline;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "customer_notes", length = 1000)
    private String customerNotes;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(length = 3)
    private String currency = "TRY";

    @Column(name = "exchange_rate")
    private Double exchangeRate = 1.0;
}
