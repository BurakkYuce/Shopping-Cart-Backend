package com.datapulse.service;

import com.datapulse.dto.response.AnalyticsCustomerResponse;
import com.datapulse.dto.response.AnalyticsProductResponse;
import com.datapulse.dto.response.AnalyticsSalesResponse;
import com.datapulse.exception.UnauthorizedAccessException;
import com.datapulse.model.Category;
import com.datapulse.model.CustomerProfile;
import com.datapulse.model.Order;
import com.datapulse.model.OrderItem;
import com.datapulse.model.Product;
import com.datapulse.model.Review;
import com.datapulse.model.RoleType;
import com.datapulse.model.Store;
import com.datapulse.repository.CategoryRepository;
import com.datapulse.repository.CustomerProfileRepository;
import com.datapulse.repository.OrderItemRepository;
import com.datapulse.repository.OrderRepository;
import com.datapulse.repository.ProductRepository;
import com.datapulse.repository.ReviewRepository;
import com.datapulse.repository.StoreRepository;
import com.datapulse.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final ReviewRepository reviewRepository;

    private UserDetailsImpl getCurrentUser(Authentication auth) {
        return (UserDetailsImpl) auth.getPrincipal();
    }

    public AnalyticsSalesResponse getSalesAnalytics(LocalDateTime from, LocalDateTime to, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        LocalDateTime effectiveFrom = from != null ? from : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime effectiveTo = to != null ? to : LocalDateTime.now();

        List<Order> orders;
        if (role == RoleType.INDIVIDUAL) {
            orders = orderRepository.findByUserIdAndCreatedAtBetween(currentUser.getId(), effectiveFrom, effectiveTo);
        } else if (role == RoleType.CORPORATE) {
            List<String> storeIds = storeRepository.findByOwnerId(currentUser.getId())
                    .stream().map(Store::getId).toList();
            orders = orderRepository.findByStoreIdInAndCreatedAtBetween(storeIds, effectiveFrom, effectiveTo);
        } else {
            orders = orderRepository.findByCreatedAtBetween(effectiveFrom, effectiveTo);
        }

        double totalRevenue = orders.stream()
                .mapToDouble(o -> o.getGrandTotal() != null ? o.getGrandTotal() : 0.0)
                .sum();
        long orderCount = orders.size();
        double avgOrderValue = orderCount > 0 ? totalRevenue / orderCount : 0.0;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, Double> revenueByDay = orders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getGrandTotal() != null)
                .collect(Collectors.groupingBy(
                        o -> o.getCreatedAt().toLocalDate().format(formatter),
                        Collectors.summingDouble(Order::getGrandTotal)
                ));

        AnalyticsSalesResponse response = new AnalyticsSalesResponse();
        response.setTotalRevenue(totalRevenue);
        response.setOrderCount(orderCount);
        response.setAverageOrderValue(avgOrderValue);
        response.setRevenueByDay(revenueByDay);
        response.setFromDate(effectiveFrom.toLocalDate().format(formatter));
        response.setToDate(effectiveTo.toLocalDate().format(formatter));

        return response;
    }

    public AnalyticsCustomerResponse getCustomerAnalytics(Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        if (currentUser.getRole() != RoleType.ADMIN) {
            throw new UnauthorizedAccessException("Access denied: ADMIN role required for customer analytics");
        }

        List<CustomerProfile> profiles = customerProfileRepository.findAll();

        double avgAge = profiles.stream()
                .filter(p -> p.getAge() != null)
                .mapToInt(CustomerProfile::getAge)
                .average()
                .orElse(0.0);

        Map<String, Double> spendByMembership = profiles.stream()
                .filter(p -> p.getMembershipType() != null && p.getTotalSpend() != null)
                .collect(Collectors.groupingBy(
                        CustomerProfile::getMembershipType,
                        Collectors.averagingDouble(CustomerProfile::getTotalSpend)
                ));

        Map<String, Long> satisfactionDistribution = profiles.stream()
                .filter(p -> p.getSatisfactionLevel() != null)
                .collect(Collectors.groupingBy(
                        CustomerProfile::getSatisfactionLevel,
                        Collectors.counting()
                ));

        Map<String, Long> cityCounts = profiles.stream()
                .filter(p -> p.getCity() != null)
                .collect(Collectors.groupingBy(
                        CustomerProfile::getCity,
                        Collectors.counting()
                ));

        Map<String, Long> topCities = cityCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        AnalyticsCustomerResponse response = new AnalyticsCustomerResponse();
        response.setAverageAge(avgAge);
        response.setSpendByMembership(spendByMembership);
        response.setSatisfactionDistribution(satisfactionDistribution);
        response.setTopCities(topCities);

        return response;
    }

    public AnalyticsProductResponse getProductAnalytics(String storeId, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        List<Product> products;
        if (role == RoleType.CORPORATE) {
            List<String> storeIds = storeRepository.findByOwnerId(currentUser.getId())
                    .stream().map(Store::getId).toList();
            products = productRepository.findByStoreIdIn(storeIds);
        } else if (role == RoleType.ADMIN) {
            products = productRepository.findAll();
        } else {
            products = productRepository.findAll();
        }

        if (storeId != null && !storeId.isBlank()) {
            products = products.stream()
                    .filter(p -> storeId.equals(p.getStoreId()))
                    .toList();
        }

        List<String> productIds = products.stream().map(Product::getId).toList();
        Map<String, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<OrderItem> allItems = orderItemRepository.findAll().stream()
                .filter(item -> productIds.contains(item.getProductId()))
                .toList();

        // Group order items by productId
        Map<String, Long> quantityByProduct = allItems.stream()
                .filter(item -> item.getQuantity() != null)
                .collect(Collectors.groupingBy(
                        OrderItem::getProductId,
                        Collectors.summingLong(OrderItem::getQuantity)
                ));

        Map<String, Double> revenueByProduct = allItems.stream()
                .filter(item -> item.getPrice() != null)
                .collect(Collectors.groupingBy(
                        OrderItem::getProductId,
                        Collectors.summingDouble(OrderItem::getPrice)
                ));

        List<AnalyticsProductResponse.TopProduct> topSellingProducts = quantityByProduct.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .map(entry -> {
                    AnalyticsProductResponse.TopProduct tp = new AnalyticsProductResponse.TopProduct();
                    tp.setProductId(entry.getKey());
                    Product p = productMap.get(entry.getKey());
                    tp.setProductName(p != null ? p.getName() : "Unknown");
                    tp.setTotalQuantity(entry.getValue());
                    tp.setTotalRevenue(revenueByProduct.getOrDefault(entry.getKey(), 0.0));
                    return tp;
                })
                .toList();

        // avgRatingByCategory — bulk fetch to avoid N+1
        List<Category> categories = categoryRepository.findAll();
        Map<String, String> categoryNameById = categories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        List<Review> allReviews = reviewRepository.findByProductIdIn(productIds);
        Map<String, List<Review>> reviewsByProduct = allReviews.stream()
                .collect(Collectors.groupingBy(Review::getProductId));

        Map<String, List<Double>> ratingsByCategory = new HashMap<>();
        for (Product product : products) {
            if (product.getCategoryId() == null) continue;
            String catName = categoryNameById.getOrDefault(product.getCategoryId(), product.getCategoryId());
            List<Review> reviews = reviewsByProduct.getOrDefault(product.getId(), List.of());
            for (Review review : reviews) {
                if (review.getStarRating() != null) {
                    ratingsByCategory.computeIfAbsent(catName, k -> new ArrayList<>())
                            .add(review.getStarRating().doubleValue());
                }
            }
        }

        Map<String, Double> avgRatingByCategory = ratingsByCategory.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0)
                ));

        AnalyticsProductResponse response = new AnalyticsProductResponse();
        response.setTopSellingProducts(topSellingProducts);
        response.setAvgRatingByCategory(avgRatingByCategory);

        return response;
    }
}
