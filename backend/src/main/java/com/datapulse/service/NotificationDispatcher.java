package com.datapulse.service;

import com.datapulse.model.NotificationPreference;
import com.datapulse.model.User;
import com.datapulse.repository.NotificationPreferenceRepository;
import com.datapulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationPreferenceRepository prefRepo;
    private final UserRepository userRepo;
    private final MailService mailService;

    /** One user, checks their orderUpdates flag. Silent no-op if disabled/missing. */
    public void dispatchOrderStatus(String userId, String orderId, String newStatus) {
        NotificationPreference pref = prefRepo.findById(userId).orElse(null);
        // Default is enabled (seeded true) — so absent row also sends.
        if (pref != null && !Boolean.TRUE.equals(pref.getOrderUpdates())) return;
        userRepo.findById(userId).ifPresent(u ->
                mailService.sendOrderStatusEmail(u.getEmail(), orderId, newStatus));
    }

    /** Fanout to everyone who opted into new-arrival emails. */
    public void dispatchNewArrival(String productId, String productName) {
        List<String> userIds = prefRepo.findUserIdsWithNewArrivalsEnabled();
        if (userIds.isEmpty()) return;
        List<User> users = userRepo.findAllById(userIds);
        log.info("Dispatching new-arrival email to {} recipients (product {})", users.size(), productId);
        for (User u : users) {
            mailService.sendNewArrivalEmail(u.getEmail(), productName, productId);
        }
    }

    /** Fanout to everyone who opted into promo emails. */
    public void dispatchPromotion(String code, String description) {
        List<String> userIds = prefRepo.findUserIdsWithPromotionsEnabled();
        if (userIds.isEmpty()) return;
        List<User> users = userRepo.findAllById(userIds);
        log.info("Dispatching promotion email to {} recipients (code {})", users.size(), code);
        for (User u : users) {
            mailService.sendPromotionEmail(u.getEmail(), code, description);
        }
    }

    /** Fanout to everyone who opted into the weekly newsletter. */
    public void dispatchWeeklyNewsletter(int newArrivalsCount, int activePromos) {
        List<String> userIds = prefRepo.findUserIdsWithNewsletterEnabled();
        if (userIds.isEmpty()) return;
        List<User> users = userRepo.findAllById(userIds);
        log.info("Dispatching weekly newsletter to {} recipients", users.size());
        for (User u : users) {
            mailService.sendWeeklyNewsletterEmail(u.getEmail(), newArrivalsCount, activePromos);
        }
    }

    /** Notify a seller about a new order, respecting their newOrderSeller flag. */
    public void dispatchNewOrderToSeller(String sellerUserId, String orderId, String buyerName, String total) {
        NotificationPreference pref = prefRepo.findById(sellerUserId).orElse(null);
        if (pref != null && !Boolean.TRUE.equals(pref.getNewOrderSeller())) return;
        userRepo.findById(sellerUserId).ifPresent(u ->
                mailService.sendNewOrderToSellerEmail(u.getEmail(), orderId, buyerName, total));
    }

    /** Notify a seller that one of their products is running low on stock. */
    public void dispatchLowStock(String sellerUserId, String productName, int remaining, int threshold) {
        NotificationPreference pref = prefRepo.findById(sellerUserId).orElse(null);
        if (pref != null && !Boolean.TRUE.equals(pref.getLowStockAlert())) return;
        userRepo.findById(sellerUserId).ifPresent(u ->
                mailService.sendLowStockEmail(u.getEmail(), productName, remaining, threshold));
    }

    /** Notify a seller when a new review lands on one of their products. */
    public void dispatchNewReview(String sellerUserId, String productName, int stars, String preview) {
        NotificationPreference pref = prefRepo.findById(sellerUserId).orElse(null);
        if (pref != null && !Boolean.TRUE.equals(pref.getNewReviewAlert())) return;
        userRepo.findById(sellerUserId).ifPresent(u ->
                mailService.sendNewReviewEmail(u.getEmail(), productName, stars, preview));
    }

    /** Send a weekly digest to a single seller, respecting their weeklyStoreDigest flag. */
    public void dispatchWeeklyStoreDigest(String sellerUserId, String storeName,
                                          long orders, String revenue, long newReviews, long lowStockCount) {
        NotificationPreference pref = prefRepo.findById(sellerUserId).orElse(null);
        if (pref != null && !Boolean.TRUE.equals(pref.getWeeklyStoreDigest())) return;
        userRepo.findById(sellerUserId).ifPresent(u ->
                mailService.sendWeeklyStoreDigestEmail(u.getEmail(), storeName,
                        orders, revenue, newReviews, lowStockCount));
    }
}
