package com.datapulse.service;

import com.datapulse.model.Order;
import com.datapulse.model.Product;
import com.datapulse.model.Store;
import com.datapulse.model.enums.OrderStatus;
import com.datapulse.repository.CouponRepository;
import com.datapulse.repository.OrderRepository;
import com.datapulse.repository.ProductRepository;
import com.datapulse.repository.ReviewRepository;
import com.datapulse.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class WeeklyDigestScheduler {

    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final StoreRepository storeRepository;
    private final NotificationDispatcher notificationDispatcher;

    /** Customer weekly newsletter — Mondays at 09:00 Europe/Istanbul. */
    @Scheduled(cron = "0 0 9 ? * MON", zone = "Europe/Istanbul")
    public void sendCustomerNewsletter() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        int newArrivals = (int) productRepository.findAll().stream()
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(weekAgo))
                .count();
        int activePromos = couponRepository.findAllByActiveTrue().size();
        log.info("Weekly newsletter: newArrivals={}, activePromos={}", newArrivals, activePromos);
        notificationDispatcher.dispatchWeeklyNewsletter(newArrivals, activePromos);
    }

    /** Seller weekly store digest — Mondays at 09:30 Europe/Istanbul. */
    @Scheduled(cron = "0 30 9 ? * MON", zone = "Europe/Istanbul")
    public void sendSellerStoreDigests() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        LocalDate reviewCutoff = LocalDate.now().minusDays(7);
        List<Store> stores = storeRepository.findAll();
        log.info("Weekly store digest: evaluating {} stores", stores.size());

        for (Store store : stores) {
            if (store.getOwnerId() == null) continue;
            String storeId = store.getId();

            List<Order> orders = orderRepository.findByStoreIdInAndCreatedAtBetween(
                    List.of(storeId), weekAgo, LocalDateTime.now());
            long orderCount = orders.size();
            BigDecimal revenue = orders.stream()
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                    .map(o -> BigDecimal.valueOf(o.getGrandTotal() == null ? 0.0 : o.getGrandTotal()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            List<Product> products = productRepository.findByStoreId(storeId);
            Set<String> productIds = products.stream().map(Product::getId).collect(Collectors.toSet());
            long newReviews = productIds.isEmpty() ? 0 : reviewRepository.findByProductIdIn(List.copyOf(productIds))
                    .stream()
                    .filter(r -> r.getReviewDate() != null && !r.getReviewDate().isBefore(reviewCutoff))
                    .count();
            long lowStockCount = productRepository.findLowStockByStoreId(storeId).size();

            notificationDispatcher.dispatchWeeklyStoreDigest(
                    store.getOwnerId(), store.getName(),
                    orderCount, revenue.toPlainString() + " TL", newReviews, lowStockCount);
        }
    }

}
