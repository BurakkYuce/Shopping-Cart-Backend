package com.datapulse.service;

import com.datapulse.dto.request.CreateReviewRequest;
import com.datapulse.dto.response.ReviewResponse;
import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.exception.UnauthorizedAccessException;
import com.datapulse.model.Review;
import com.datapulse.model.RoleType;
import com.datapulse.repository.ProductRepository;
import com.datapulse.repository.ReviewRepository;
import com.datapulse.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    private UserDetailsImpl getCurrentUser(Authentication auth) {
        return (UserDetailsImpl) auth.getPrincipal();
    }

    public List<ReviewResponse> getByProductId(String productId) {
        return reviewRepository.findByProductId(productId)
                .stream().map(ReviewResponse::from).toList();
    }

    public ReviewResponse createReview(CreateReviewRequest req, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);

        // Verify product exists
        productRepository.findById(req.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product", req.getProductId()));

        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        Review review = new Review();
        review.setId(id);
        review.setUserId(currentUser.getId());
        review.setProductId(req.getProductId());
        review.setStarRating(req.getStarRating());
        review.setReviewHeadline(req.getReviewHeadline());
        review.setReviewText(req.getReviewText());
        review.setHelpfulVotes(0);
        review.setTotalVotes(0);
        review.setSentiment("neutral");
        review.setVerifiedPurchase("N");
        review.setReviewDate(LocalDate.now());

        reviewRepository.save(review);
        return ReviewResponse.from(review);
    }

    public void deleteReview(String id, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Review", id));

        boolean isAdmin = role == RoleType.ADMIN;
        boolean isOwner = review.getUserId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new UnauthorizedAccessException("Access denied: you can only delete your own reviews");
        }

        reviewRepository.delete(review);
    }
}
