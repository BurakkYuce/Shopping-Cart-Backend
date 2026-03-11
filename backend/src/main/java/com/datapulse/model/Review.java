package com.datapulse.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

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

    @Column(name = "product_id", nullable = false)
    private String productId;

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
}
