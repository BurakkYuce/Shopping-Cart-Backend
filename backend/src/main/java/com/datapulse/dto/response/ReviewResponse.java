package com.datapulse.dto.response;

import com.datapulse.model.Review;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReviewResponse {
    private String id;
    private String userId;
    private String productId;
    private Integer starRating;
    private Integer helpfulVotes;
    private Integer totalVotes;
    private String reviewHeadline;
    private String reviewText;
    private String sentiment;
    private String verifiedPurchase;
    private LocalDate reviewDate;

    public static ReviewResponse from(Review review) {
        ReviewResponse r = new ReviewResponse();
        r.id = review.getId();
        r.userId = review.getUserId();
        r.productId = review.getProductId();
        r.starRating = review.getStarRating();
        r.helpfulVotes = review.getHelpfulVotes();
        r.totalVotes = review.getTotalVotes();
        r.reviewHeadline = review.getReviewHeadline();
        r.reviewText = review.getReviewText();
        r.sentiment = review.getSentiment();
        r.verifiedPurchase = review.getVerifiedPurchase();
        r.reviewDate = review.getReviewDate();
        return r;
    }
}
