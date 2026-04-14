package com.datapulse.service;

import com.datapulse.dto.request.CreateProductRequest;
import com.datapulse.dto.response.ProductResponse;
import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.exception.UnauthorizedAccessException;
import com.datapulse.logging.LogEventPublisher;
import com.datapulse.logging.LogEventType;
import com.datapulse.model.Product;
import com.datapulse.model.Review;
import com.datapulse.model.RoleType;
import com.datapulse.model.Store;
import com.datapulse.repository.CartItemRepository;
import com.datapulse.repository.OrderItemRepository;
import com.datapulse.repository.ProductAttributeRepository;
import com.datapulse.repository.ProductRepository;
import com.datapulse.repository.ReviewRepository;
import com.datapulse.repository.StoreRepository;
import com.datapulse.repository.WishlistItemRepository;
import com.datapulse.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final StoreRepository storeRepository;
    private final CartItemRepository cartItemRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final LogEventPublisher logEventPublisher;

    private UserDetailsImpl getCurrentUser(Authentication auth) {
        return (UserDetailsImpl) auth.getPrincipal();
    }

    /** Batch-enrich a page of products with review aggregates computed from actual review rows.
     *  Products with zero reviews end up with rating=null so the UI can show "no reviews yet". */
    private Page<ProductResponse> enrichPageWithReviews(Page<Product> page) {
        List<String> productIds = page.getContent().stream().map(Product::getId).toList();
        Map<String, List<Review>> reviewsByProduct = productIds.isEmpty()
                ? Map.of()
                : reviewRepository.findByProductIdIn(productIds).stream()
                        .collect(java.util.stream.Collectors.groupingBy(Review::getProductId));
        return page.map(p -> {
            List<Review> revs = reviewsByProduct.getOrDefault(p.getId(), List.of());
            return ProductResponse.from(p).withReviewAggregates(averageStarRating(revs), revs.size());
        });
    }

    private Double averageStarRating(List<Review> reviews) {
        if (reviews == null || reviews.isEmpty()) return null;
        return reviews.stream()
                .map(Review::getStarRating)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }

    public Page<ProductResponse> getProducts(Authentication auth, Pageable pageable) {
        if (auth == null || !auth.isAuthenticated()) {
            return enrichPageWithReviews(productRepository.findAll(pageable));
        }

        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        if (role == RoleType.CORPORATE) {
            List<String> storeIds = storeRepository.findByOwnerId(currentUser.getId())
                    .stream().map(Store::getId).toList();
            return enrichPageWithReviews(productRepository.findByStoreIdIn(storeIds, pageable));
        }

        return enrichPageWithReviews(productRepository.findAll(pageable));
    }

    public Page<ProductResponse> searchProducts(String query, String categoryId, String brand, Double minPrice, Double maxPrice, String storeId, Pageable pageable) {
        return enrichPageWithReviews(
                productRepository.search(query, categoryId, brand, minPrice, maxPrice, storeId, pageable));
    }

    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product", id));

        logEventPublisher.publish(
                LogEventType.PRODUCT_VIEWED,
                null,
                null,
                Map.of("productId", id, "productName", product.getName())
        );

        var attrs = productAttributeRepository.findByProductId(id);
        var reviews = reviewRepository.findByProductId(id);
        return ProductResponse.from(product, attrs)
                .withReviewAggregates(averageStarRating(reviews), reviews.size());
    }

    public ProductResponse createProduct(CreateProductRequest req, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        if (role != RoleType.CORPORATE && role != RoleType.ADMIN) {
            throw new UnauthorizedAccessException("Access denied: CORPORATE or ADMIN role required");
        }

        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new EntityNotFoundException("Store", req.getStoreId()));
        if (role == RoleType.CORPORATE && !store.getOwnerId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("Access denied: you do not own this store");
        }
        if (store.getStatus() != com.datapulse.model.enums.StoreStatus.ACTIVE) {
            throw new UnauthorizedAccessException(
                    "Store is " + store.getStatus().name() + " — cannot create products");
        }

        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        Product product = new Product();
        product.setId(id);
        product.setStoreId(req.getStoreId());
        product.setCategoryId(req.getCategoryId());
        product.setSku(req.getSku());
        product.setName(req.getName());
        product.setUnitPrice(req.getUnitPrice());
        product.setDescription(req.getDescription());
        product.setImageUrl(req.getImageUrl());
        if (req.getStockQuantity() != null) product.setStockQuantity(req.getStockQuantity());

        productRepository.save(product);
        return ProductResponse.from(product);
    }

    public ProductResponse updateProduct(String id, CreateProductRequest req, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product", id));

        if (role == RoleType.CORPORATE) {
            Store store = storeRepository.findById(product.getStoreId())
                    .orElseThrow(() -> new EntityNotFoundException("Store", product.getStoreId()));
            if (!store.getOwnerId().equals(currentUser.getId())) {
                throw new UnauthorizedAccessException("Access denied: you do not own this product's store");
            }
        } else if (role != RoleType.ADMIN) {
            throw new UnauthorizedAccessException("Access denied: insufficient permissions");
        }

        if (req.getStoreId() != null) product.setStoreId(req.getStoreId());
        if (req.getCategoryId() != null) product.setCategoryId(req.getCategoryId());
        if (req.getSku() != null) product.setSku(req.getSku());
        if (req.getName() != null) product.setName(req.getName());
        if (req.getUnitPrice() != null) product.setUnitPrice(req.getUnitPrice());
        if (req.getDescription() != null) product.setDescription(req.getDescription());
        if (req.getImageUrl() != null) product.setImageUrl(req.getImageUrl());
        if (req.getStockQuantity() != null) product.setStockQuantity(req.getStockQuantity());

        productRepository.save(product);
        return ProductResponse.from(product);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteProduct(String id, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product", id));

        if (role == RoleType.CORPORATE) {
            Store store = storeRepository.findById(product.getStoreId())
                    .orElseThrow(() -> new EntityNotFoundException("Store", product.getStoreId()));
            if (!store.getOwnerId().equals(currentUser.getId())) {
                throw new UnauthorizedAccessException("Access denied: you do not own this product's store");
            }
        } else if (role != RoleType.ADMIN) {
            throw new UnauthorizedAccessException("Access denied: insufficient permissions");
        }

        // Clean up FK references before deleting product
        cartItemRepository.deleteByProductId(id);
        wishlistItemRepository.deleteByProductId(id);
        reviewRepository.deleteByProductId(id);
        orderItemRepository.deleteByProductId(id);
        productAttributeRepository.deleteByProductId(id);

        productRepository.delete(product);
    }
}
