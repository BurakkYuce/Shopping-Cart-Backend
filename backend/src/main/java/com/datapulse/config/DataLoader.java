package com.datapulse.config;

import com.datapulse.logging.LogEventPublisher;
import com.datapulse.logging.LogEventType;
import com.datapulse.model.*;
import com.datapulse.repository.*;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.dataloader.enabled", havingValue = "true")
public class DataLoader implements CommandLineRunner {

    private static final int BATCH_SIZE = 500;

    @Value("${app.datasets.path}")
    private String datasetsPath;

    @Value("${app.dataloader.seed-password}")
    private String seedPassword;

    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShipmentRepository shipmentRepository;
    private final ReviewRepository reviewRepository;
    private final LogEventPublisher logEventPublisher;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Data already loaded, skipping DataLoader.");
            return;
        }
        log.info("Starting data load from: {}", datasetsPath);
        try {
            log.info("Disabling FK constraints for bulk data load...");
            jdbcTemplate.execute("SET session_replication_role = 'replica'");

            loadCategories();
            loadUsers();
            loadStores();
            loadCustomerProfiles();
            loadProducts();
            loadOrders();
            loadOrderItems();
            loadShipments();
            loadReviews();

            jdbcTemplate.execute("SET session_replication_role = 'origin'");
            log.info("FK constraints re-enabled.");

            logEventPublisher.publish(LogEventType.DATA_LOADED, null, null,
                    Map.of("message", "All CSV data loaded successfully"));
            log.info("Data loading complete.");
        } catch (Exception e) {
            jdbcTemplate.execute("SET session_replication_role = 'origin'");
            log.error("Data loading failed", e);
        }
    }

    // ─── CSV Row classes ────────────────────────────────────────────────────────

    public static class CategoryRow {
        @CsvBindByName(column = "id") public String id;
        @CsvBindByName(column = "name") public String name;
        @CsvBindByName(column = "parent_id") public String parentId;
    }

    public static class UserRow {
        @CsvBindByName(column = "id") public String id;
        @CsvBindByName(column = "email") public String email;
        @CsvBindByName(column = "password_hash") public String passwordHash;
        @CsvBindByName(column = "role_type") public String roleType;
        @CsvBindByName(column = "gender") public String gender;
    }

    public static class StoreRow {
        @CsvBindByName(column = "id") public String id;
        @CsvBindByName(column = "owner_id") public String ownerId;
        @CsvBindByName(column = "name") public String name;
        @CsvBindByName(column = "status") public String status;
    }

    public static class CustomerProfileRow {
        @CsvBindByName(column = "id") public String id;
        @CsvBindByName(column = "user_id") public String userId;
        @CsvBindByName(column = "age") public String age;
        @CsvBindByName(column = "city") public String city;
        @CsvBindByName(column = "membership_type") public String membershipType;
        @CsvBindByName(column = "total_spend") public String totalSpend;
        @CsvBindByName(column = "items_purchased") public String itemsPurchased;
        @CsvBindByName(column = "average_rating") public String averageRating;
        @CsvBindByName(column = "satisfaction_level") public String satisfactionLevel;
    }

    public static class ProductRow {
        @CsvBindByName(column = "id") public String id;
        @CsvBindByName(column = "store_id") public String storeId;
        @CsvBindByName(column = "category_id") public String categoryId;
        @CsvBindByName(column = "sku") public String sku;
        @CsvBindByName(column = "name") public String name;
        @CsvBindByName(column = "unit_price") public String unitPrice;
        @CsvBindByName(column = "description") public String description;
    }

    public static class OrderRow {
        @CsvBindByName(column = "id") public String id;
        @CsvBindByName(column = "user_id") public String userId;
        @CsvBindByName(column = "store_id") public String storeId;
        @CsvBindByName(column = "status") public String status;
        @CsvBindByName(column = "grand_total") public String grandTotal;
        @CsvBindByName(column = "created_at") public String createdAt;
        @CsvBindByName(column = "payment_method") public String paymentMethod;
    }

    public static class OrderItemRow {
        @CsvBindByName(column = "id") public String id;
        @CsvBindByName(column = "order_id") public String orderId;
        @CsvBindByName(column = "product_id") public String productId;
        @CsvBindByName(column = "quantity") public String quantity;
        @CsvBindByName(column = "price") public String price;
    }

    public static class ShipmentRow {
        @CsvBindByName(column = "id") public String id;
        @CsvBindByName(column = "order_id") public String orderId;
        @CsvBindByName(column = "warehouse") public String warehouse;
        @CsvBindByName(column = "mode") public String mode;
        @CsvBindByName(column = "status") public String status;
        @CsvBindByName(column = "customer_care_calls") public String customerCareCalls;
        @CsvBindByName(column = "customer_rating") public String customerRating;
        @CsvBindByName(column = "weight_gms") public String weightGms;
    }

    public static class ReviewRow {
        @CsvBindByName(column = "id") public String id;
        @CsvBindByName(column = "user_id") public String userId;
        @CsvBindByName(column = "product_id") public String productId;
        @CsvBindByName(column = "star_rating") public String starRating;
        @CsvBindByName(column = "helpful_votes") public String helpfulVotes;
        @CsvBindByName(column = "total_votes") public String totalVotes;
        @CsvBindByName(column = "review_headline") public String reviewHeadline;
        @CsvBindByName(column = "review_text") public String reviewText;
        @CsvBindByName(column = "sentiment") public String sentiment;
        @CsvBindByName(column = "verified_purchase") public String verifiedPurchase;
        @CsvBindByName(column = "review_date") public String reviewDate;
    }

    // ─── Loaders ────────────────────────────────────────────────────────────────

    private void loadCategories() throws Exception {
        List<CategoryRow> rows = parseCsv(datasetsPath + "categories.csv", CategoryRow.class);
        List<Category> rootCategories = new ArrayList<>();
        List<Category> childCategories = new ArrayList<>();

        for (CategoryRow r : rows) {
            Category cat = new Category();
            cat.setId(r.id);
            cat.setName(r.name);
            cat.setParentId(isBlank(r.parentId) ? null : r.parentId);
            if (isBlank(r.parentId)) {
                rootCategories.add(cat);
            } else {
                childCategories.add(cat);
            }
        }
        saveBatched(rootCategories, categoryRepository);
        saveBatched(childCategories, categoryRepository);
        log.info("Loaded {} categories", rows.size());
    }

    private void loadUsers() throws Exception {
        List<UserRow> rows = parseCsv(datasetsPath + "users.csv", UserRow.class);
        String sharedHash = passwordEncoder.encode(seedPassword);
        List<User> users = new ArrayList<>();
        for (UserRow r : rows) {
            User u = new User();
            u.setId(r.id);
            u.setEmail(r.email);
            u.setPasswordHash(sharedHash);
            u.setRoleType(parseRoleType(r.roleType));
            u.setGender(r.gender);
            users.add(u);
        }
        saveBatched(users, userRepository);
        log.info("Loaded {} users", users.size());
    }

    private void loadStores() throws Exception {
        List<StoreRow> rows = parseCsv(datasetsPath + "stores.csv", StoreRow.class);
        List<Store> stores = new ArrayList<>();
        for (StoreRow r : rows) {
            Store s = new Store();
            s.setId(r.id);
            s.setOwnerId(r.ownerId);
            s.setName(r.name);
            s.setStatus(r.status);
            stores.add(s);
        }
        saveBatched(stores, storeRepository);
        log.info("Loaded {} stores", stores.size());
    }

    private void loadCustomerProfiles() throws Exception {
        List<CustomerProfileRow> rows = parseCsv(datasetsPath + "customer_profiles.csv", CustomerProfileRow.class);
        List<CustomerProfile> profiles = new ArrayList<>();
        for (CustomerProfileRow r : rows) {
            CustomerProfile cp = new CustomerProfile();
            cp.setId(r.id);
            cp.setUserId(r.userId);
            cp.setAge(parseIntSafe(r.age));
            cp.setCity(r.city);
            cp.setMembershipType(r.membershipType);
            cp.setTotalSpend(parseDoubleSafe(r.totalSpend));
            cp.setItemsPurchased(parseIntSafe(r.itemsPurchased));
            cp.setAverageRating(parseDoubleSafe(r.averageRating));
            cp.setSatisfactionLevel(r.satisfactionLevel);
            profiles.add(cp);
        }
        saveBatched(profiles, customerProfileRepository);
        log.info("Loaded {} customer profiles", profiles.size());
    }

    private void loadProducts() throws Exception {
        List<ProductRow> rows = parseCsv(datasetsPath + "products.csv", ProductRow.class);
        List<Product> products = new ArrayList<>();
        for (ProductRow r : rows) {
            Product p = new Product();
            p.setId(r.id);
            p.setStoreId(r.storeId);
            p.setCategoryId(isBlank(r.categoryId) ? null : r.categoryId);
            p.setSku(r.sku);
            p.setName(r.name);
            p.setUnitPrice(parseDoubleSafe(r.unitPrice));
            p.setDescription(r.description);
            products.add(p);
        }
        saveBatched(products, productRepository);
        log.info("Loaded {} products", products.size());
    }

    private void loadOrders() throws Exception {
        List<OrderRow> rows = parseCsv(datasetsPath + "orders.csv", OrderRow.class);
        List<Order> orders = new ArrayList<>();
        for (OrderRow r : rows) {
            Order o = new Order();
            o.setId(r.id);
            o.setUserId(r.userId);
            o.setStoreId(r.storeId);
            o.setStatus(r.status);
            o.setGrandTotal(parseDoubleSafe(r.grandTotal));
            o.setCreatedAt(parseDateTime(r.createdAt));
            o.setPaymentMethod(r.paymentMethod);
            orders.add(o);
        }
        saveBatched(orders, orderRepository);
        log.info("Loaded {} orders", orders.size());
    }

    private void loadOrderItems() throws Exception {
        List<OrderItemRow> rows = parseCsv(datasetsPath + "order_items.csv", OrderItemRow.class);
        List<OrderItem> items = new ArrayList<>();
        for (OrderItemRow r : rows) {
            OrderItem oi = new OrderItem();
            oi.setId(r.id);
            oi.setOrderId(r.orderId);
            oi.setProductId(r.productId);
            oi.setQuantity(parseIntSafe(r.quantity));
            oi.setPrice(parseDoubleSafe(r.price));
            items.add(oi);
        }
        saveBatched(items, orderItemRepository);
        log.info("Loaded {} order items", items.size());
    }

    private void loadShipments() throws Exception {
        List<ShipmentRow> rows = parseCsv(datasetsPath + "shipments.csv", ShipmentRow.class);
        List<Shipment> shipments = new ArrayList<>();
        for (ShipmentRow r : rows) {
            Shipment s = new Shipment();
            s.setId(r.id);
            s.setOrderId(r.orderId);
            s.setWarehouse(r.warehouse);
            s.setMode(r.mode);
            s.setStatus(r.status);
            s.setCustomerCareCalls(parseIntSafe(r.customerCareCalls));
            s.setCustomerRating(parseIntSafe(r.customerRating));
            s.setWeightGms(parseIntSafe(r.weightGms));
            shipments.add(s);
        }
        saveBatched(shipments, shipmentRepository);
        log.info("Loaded {} shipments", shipments.size());
    }

    private void loadReviews() throws Exception {
        List<ReviewRow> rows = parseCsv(datasetsPath + "reviews.csv", ReviewRow.class);
        List<Review> reviews = new ArrayList<>();
        for (ReviewRow r : rows) {
            Review rv = new Review();
            rv.setId(r.id);
            rv.setUserId(r.userId);
            rv.setProductId(r.productId);
            rv.setStarRating(parseIntSafe(r.starRating));
            rv.setHelpfulVotes(parseIntSafe(r.helpfulVotes));
            rv.setTotalVotes(parseIntSafe(r.totalVotes));
            rv.setReviewHeadline(r.reviewHeadline);
            rv.setReviewText(r.reviewText);
            rv.setSentiment(r.sentiment);
            rv.setVerifiedPurchase(r.verifiedPurchase);
            rv.setReviewDate(parseDate(r.reviewDate));
            reviews.add(rv);
        }
        saveBatched(reviews, reviewRepository);
        log.info("Loaded {} reviews", reviews.size());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private <T> List<T> parseCsv(String filePath, Class<T> clazz) throws Exception {
        try (FileReader reader = new FileReader(filePath)) {
            return new CsvToBeanBuilder<T>(reader)
                    .withType(clazz)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
        }
    }

    private <T, ID> void saveBatched(List<T> entities, org.springframework.data.jpa.repository.JpaRepository<T, ID> repo) {
        for (int i = 0; i < entities.size(); i += BATCH_SIZE) {
            repo.saveAll(entities.subList(i, Math.min(i + BATCH_SIZE, entities.size())));
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private Integer parseIntSafe(String s) {
        if (isBlank(s)) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private Double parseDoubleSafe(String s) {
        if (isBlank(s)) return null;
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private LocalDateTime parseDateTime(String s) {
        if (isBlank(s)) return null;
        try { return LocalDateTime.parse(s.trim()); } catch (Exception e) { return null; }
    }

    private LocalDate parseDate(String s) {
        if (isBlank(s)) return null;
        try { return LocalDate.parse(s.trim()); } catch (Exception e) { return null; }
    }

    private RoleType parseRoleType(String s) {
        if (isBlank(s)) return RoleType.INDIVIDUAL;
        return switch (s.trim().toLowerCase()) {
            case "admin" -> RoleType.ADMIN;
            case "corporate" -> RoleType.CORPORATE;
            default -> RoleType.INDIVIDUAL;
        };
    }
}
