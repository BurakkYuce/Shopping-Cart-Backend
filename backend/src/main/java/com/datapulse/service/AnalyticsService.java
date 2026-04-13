package com.datapulse.service;

import com.datapulse.dto.response.AnalyticsCustomerResponse;
import com.datapulse.dto.response.AnalyticsProductResponse;
import com.datapulse.dto.response.AnalyticsSalesResponse;
import com.datapulse.dto.response.CustomerSegmentResponse;
import com.datapulse.dto.response.StoreKpiResponse;
import com.datapulse.exception.EntityNotFoundException;
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
        RoleType role = currentUser.getRole();

        List<CustomerProfile> profiles;
        if (role == RoleType.INDIVIDUAL) {
            profiles = customerProfileRepository.findByUserId(currentUser.getId())
                    .map(List::of).orElse(List.of());
        } else {
            profiles = customerProfileRepository.findAll();
        }

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

    public StoreKpiResponse getStoreKpis(String storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("Store", storeId));

        List<Order> orders = orderRepository.findByStoreId(storeId);
        double totalRevenue = orders.stream()
                .mapToDouble(o -> o.getGrandTotal() != null ? o.getGrandTotal() : 0.0).sum();
        long orderCount = orders.size();
        double avgOrderValue = orderCount > 0 ? totalRevenue / orderCount : 0.0;
        long customerCount = orders.stream().map(Order::getUserId).distinct().count();

        List<Product> products = productRepository.findByStoreId(storeId);
        List<String> productIds = products.stream().map(Product::getId).toList();
        Map<String, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<OrderItem> items = orderItemRepository.findAll().stream()
                .filter(i -> productIds.contains(i.getProductId())).toList();

        Map<String, Long> qtyByProduct = items.stream()
                .filter(i -> i.getQuantity() != null)
                .collect(Collectors.groupingBy(OrderItem::getProductId, Collectors.summingLong(OrderItem::getQuantity)));
        Map<String, Double> revByProduct = items.stream()
                .filter(i -> i.getPrice() != null)
                .collect(Collectors.groupingBy(OrderItem::getProductId, Collectors.summingDouble(OrderItem::getPrice)));

        List<StoreKpiResponse.TopProduct> topProducts = qtyByProduct.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .map(e -> {
                    StoreKpiResponse.TopProduct tp = new StoreKpiResponse.TopProduct();
                    tp.setProductId(e.getKey());
                    Product p = productMap.get(e.getKey());
                    tp.setProductName(p != null ? p.getName() : "Unknown");
                    tp.setTotalQuantity(e.getValue());
                    tp.setTotalRevenue(revByProduct.getOrDefault(e.getKey(), 0.0));
                    return tp;
                }).toList();

        List<Review> reviews = reviewRepository.findByProductIdIn(productIds);
        Map<Integer, Long> ratingDist = reviews.stream()
                .filter(r -> r.getStarRating() != null)
                .collect(Collectors.groupingBy(Review::getStarRating, Collectors.counting()));

        StoreKpiResponse kpi = new StoreKpiResponse();
        kpi.setStoreId(storeId);
        kpi.setStoreName(store.getName());
        kpi.setTotalRevenue(totalRevenue);
        kpi.setOrderCount(orderCount);
        kpi.setAverageOrderValue(avgOrderValue);
        kpi.setCustomerCount(customerCount);
        kpi.setTopProducts(topProducts);
        kpi.setRatingDistribution(ratingDist);
        return kpi;
    }

    public CustomerSegmentResponse getCustomerSegments(String storeId, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        List<Order> orders;
        if (storeId != null && !storeId.isBlank()) {
            orders = orderRepository.findByStoreId(storeId);
        } else if (role == RoleType.CORPORATE) {
            List<String> storeIds = storeRepository.findByOwnerId(currentUser.getId())
                    .stream().map(Store::getId).toList();
            orders = orderRepository.findByStoreIdIn(storeIds);
        } else {
            orders = orderRepository.findAll();
        }

        Map<String, Double> spendByUser = orders.stream()
                .filter(o -> o.getGrandTotal() != null)
                .collect(Collectors.groupingBy(Order::getUserId, Collectors.summingDouble(Order::getGrandTotal)));

        Map<String, Long> orderCountByUser = orders.stream()
                .collect(Collectors.groupingBy(Order::getUserId, Collectors.counting()));

        List<CustomerSegmentResponse.Segment> segments = new ArrayList<>();
        long highValue = 0, medium = 0, low = 0, churnRisk = 0, newCust = 0, dormant = 0;
        double hvSpend = 0, mSpend = 0, lSpend = 0, crSpend = 0, nSpend = 0, dSpend = 0;

        for (Map.Entry<String, Double> entry : spendByUser.entrySet()) {
            double spend = entry.getValue();
            long count = orderCountByUser.getOrDefault(entry.getKey(), 0L);
            if (spend >= 1000 && count >= 5) { highValue++; hvSpend += spend; }
            else if (spend >= 300) { medium++; mSpend += spend; }
            else if (count >= 2) { low++; lSpend += spend; }
            else if (count == 1 && spend < 100) { dormant++; dSpend += spend; }
            else if (count == 1) { newCust++; nSpend += spend; }
            else { churnRisk++; crSpend += spend; }
        }

        long total = spendByUser.size();
        addSegment(segments, "HIGH_VALUE", highValue, total, hvSpend);
        addSegment(segments, "MEDIUM", medium, total, mSpend);
        addSegment(segments, "LOW", low, total, lSpend);
        addSegment(segments, "CHURN_RISK", churnRisk, total, crSpend);
        addSegment(segments, "NEW", newCust, total, nSpend);
        addSegment(segments, "DORMANT", dormant, total, dSpend);

        CustomerSegmentResponse resp = new CustomerSegmentResponse();
        resp.setSegments(segments);
        resp.setTotalCustomers(total);
        return resp;
    }

    private void addSegment(List<CustomerSegmentResponse.Segment> list, String name, long count, long total, double totalSpend) {
        CustomerSegmentResponse.Segment s = new CustomerSegmentResponse.Segment();
        s.setName(name);
        s.setCount(count);
        s.setPercentage(total > 0 ? Math.round(count * 10000.0 / total) / 100.0 : 0.0);
        s.setAvgSpend(count > 0 ? Math.round(totalSpend / count * 100.0) / 100.0 : 0.0);
        list.add(s);
    }

    public List<StoreKpiResponse> getStoreComparison(List<String> storeIds, LocalDateTime from, LocalDateTime to) {
        return storeIds.stream().map(this::getStoreKpis).toList();
    }

    public AnalyticsSalesResponse getSalesWithDrillDown(String groupBy, String storeId,
                                                        LocalDateTime from, LocalDateTime to, Authentication auth) {
        AnalyticsSalesResponse base = getSalesAnalytics(from, to, auth);

        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();
        LocalDateTime effectiveFrom = from != null ? from : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime effectiveTo = to != null ? to : LocalDateTime.now();

        List<Order> orders;
        if (role == RoleType.INDIVIDUAL) {
            orders = orderRepository.findByUserIdAndCreatedAtBetween(currentUser.getId(), effectiveFrom, effectiveTo);
        } else if (role == RoleType.CORPORATE) {
            List<String> sIds = storeRepository.findByOwnerId(currentUser.getId())
                    .stream().map(Store::getId).toList();
            orders = orderRepository.findByStoreIdInAndCreatedAtBetween(sIds, effectiveFrom, effectiveTo);
        } else {
            orders = orderRepository.findByCreatedAtBetween(effectiveFrom, effectiveTo);
        }

        if (storeId != null && !storeId.isBlank()) {
            orders = orders.stream().filter(o -> storeId.equals(o.getStoreId())).toList();
        }

        if ("week".equalsIgnoreCase(groupBy)) {
            Map<String, Double> byWeek = orders.stream()
                    .filter(o -> o.getCreatedAt() != null && o.getGrandTotal() != null)
                    .collect(Collectors.groupingBy(
                            o -> o.getCreatedAt().getYear() + "-W" + String.format("%02d",
                                    o.getCreatedAt().getDayOfYear() / 7 + 1),
                            Collectors.summingDouble(Order::getGrandTotal)));
            base.setRevenueByWeek(byWeek);
        } else if ("month".equalsIgnoreCase(groupBy)) {
            Map<String, Double> byMonth = orders.stream()
                    .filter(o -> o.getCreatedAt() != null && o.getGrandTotal() != null)
                    .collect(Collectors.groupingBy(
                            o -> o.getCreatedAt().getYear() + "-" + String.format("%02d", o.getCreatedAt().getMonthValue()),
                            Collectors.summingDouble(Order::getGrandTotal)));
            base.setRevenueByMonth(byMonth);
        } else if ("category".equalsIgnoreCase(groupBy)) {
            List<String> orderIds = orders.stream().map(Order::getId).toList();
            List<OrderItem> items = orderItemRepository.findByOrderIdIn(orderIds);
            Map<String, String> productCategory = productRepository.findAll().stream()
                    .filter(p -> p.getCategoryId() != null)
                    .collect(Collectors.toMap(Product::getId, Product::getCategoryId));
            Map<String, String> catNames = categoryRepository.findAll().stream()
                    .collect(Collectors.toMap(Category::getId, Category::getName));

            Map<String, Double> byCat = new HashMap<>();
            for (OrderItem item : items) {
                String catId = productCategory.get(item.getProductId());
                String catName = catId != null ? catNames.getOrDefault(catId, catId) : "Uncategorized";
                byCat.merge(catName, item.getPrice() != null ? item.getPrice() : 0.0, Double::sum);
            }
            base.setRevenueByCategory(byCat);
        }

        return base;
    }

    public Map<String, Object> getPlatformOverview() {
        List<Order> allOrders = orderRepository.findAll();
        List<Store> allStores = storeRepository.findAll();
        long userCount = customerProfileRepository.count();

        double totalGmv = allOrders.stream()
                .mapToDouble(o -> o.getGrandTotal() != null ? o.getGrandTotal() : 0.0)
                .sum();
        long totalOrders = allOrders.size();

        // Top 5 stores by revenue
        Map<String, Double> revenueByStore = allOrders.stream()
                .collect(Collectors.groupingBy(Order::getStoreId,
                        Collectors.summingDouble(o -> o.getGrandTotal() != null ? o.getGrandTotal() : 0.0)));
        Map<String, String> storeNames = allStores.stream()
                .collect(Collectors.toMap(Store::getId, Store::getName));

        List<Map<String, Object>> topStores = revenueByStore.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("storeId", e.getKey());
                    m.put("storeName", storeNames.getOrDefault(e.getKey(), e.getKey()));
                    m.put("revenue", Math.round(e.getValue() * 100.0) / 100.0);
                    return m;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalGmv", Math.round(totalGmv * 100.0) / 100.0);
        result.put("totalOrders", totalOrders);
        result.put("totalUsers", userCount);
        result.put("totalStores", allStores.size());
        result.put("topStores", topStores);
        return result;
    }
}
