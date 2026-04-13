package com.datapulse.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {

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

    @Column(name = "product_id", nullable = false)
    private String productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Product product;

    @Column(name = "star_rating")
    private Integer starRating;

    @Column(name = "helpful_votes")
    private Integer helpfulVotes;

    @Column(name = "total_votes")
    private Integer totalVotes;

    @Column(name = "review_headline")
    private String reviewHeadline;

    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    private String sentiment;

    @Column(name = "verified_purchase")
    private String verifiedPurchase;

    @Column(name = "review_date")
    private LocalDate reviewDate;

    @Column(name = "seller_response", length = 2000)
    private String sellerResponse;

    @Column(name = "seller_response_date")
    private java.time.LocalDateTime sellerResponseDate;
}
