package com.datapulse.service;

import com.datapulse.dto.request.CreateReviewRequest;
import com.datapulse.dto.response.ReviewResponse;
import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.exception.UnauthorizedAccessException;
import com.datapulse.model.Product;
import com.datapulse.model.Review;
import com.datapulse.model.RoleType;
import com.datapulse.model.Store;
import com.datapulse.repository.OrderItemRepository;
import com.datapulse.repository.ProductRepository;
import com.datapulse.repository.ReviewRepository;
import com.datapulse.repository.StoreRepository;
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
    private final OrderItemRepository orderItemRepository;
    private final StoreRepository storeRepository;
    private final NotificationDispatcher notificationDispatcher;

    private UserDetailsImpl getCurrentUser(Authentication auth) {
        return (UserDetailsImpl) auth.getPrincipal();
    }

    public List<ReviewResponse> getByProductId(String productId) {
        return reviewRepository.findByProductId(productId)
                .stream().map(ReviewResponse::from).toList();
    }

    public ReviewResponse createReview(CreateReviewRequest req, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        String userId = currentUser.getId();

        // Verify product exists
        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product", req.getProductId()));

        // Must have a delivered order containing this product
        if (!orderItemRepository.existsDeliveredOrderWithProduct(userId, req.getProductId())) {
            throw new IllegalArgumentException("You can only review products you have purchased and received.");
        }

        // Prevent duplicate reviews
        if (reviewRepository.existsByUserIdAndProductId(userId, req.getProductId())) {
            throw new IllegalArgumentException("You have already reviewed this product.");
        }

        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        Review review = new Review();
        review.setId(id);
        review.setUserId(userId);
        review.setProductId(req.getProductId());
        review.setStarRating(req.getStarRating());
        review.setReviewHeadline(req.getReviewHeadline());
        review.setReviewText(req.getReviewText());
        review.setHelpfulVotes(0);
        review.setTotalVotes(0);
        review.setSentiment("neutral");
        review.setVerifiedPurchase("Y");
        review.setReviewDate(LocalDate.now());

        reviewRepository.save(review);

        try {
            Store store = storeRepository.findById(product.getStoreId()).orElse(null);
            if (store != null && store.getOwnerId() != null) {
                String preview = review.getReviewText();
                if (preview != null && preview.length() > 200) preview = preview.substring(0, 200) + "…";
                notificationDispatcher.dispatchNewReview(
                        store.getOwnerId(), product.getName(), review.getStarRating(), preview);
            }
        } catch (Exception ex) {
            // Review is saved; don't fail the call if the email hiccups.
        }

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

    public ReviewResponse addSellerResponse(String reviewId, String response, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review", reviewId));

        Product product = productRepository.findById(review.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product", review.getProductId()));

        if (role == RoleType.CORPORATE) {
            Store store = storeRepository.findById(product.getStoreId())
                    .orElseThrow(() -> new EntityNotFoundException("Store", product.getStoreId()));
            if (!store.getOwnerId().equals(currentUser.getId())) {
                throw new UnauthorizedAccessException("Access denied: you do not own this product's store");
            }
        } else if (role != RoleType.ADMIN) {
            throw new UnauthorizedAccessException("Access denied: CORPORATE or ADMIN role required");
        }

        review.setSellerResponse(response);
        review.setSellerResponseDate(java.time.LocalDateTime.now());
        reviewRepository.save(review);
        return ReviewResponse.from(review);
    }
}
