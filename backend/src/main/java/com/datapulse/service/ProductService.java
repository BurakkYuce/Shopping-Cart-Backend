package com.datapulse.service;

import com.datapulse.dto.request.CreateProductRequest;
import com.datapulse.dto.response.ProductResponse;
import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.exception.UnauthorizedAccessException;
import com.datapulse.logging.LogEventPublisher;
import com.datapulse.logging.LogEventType;
import com.datapulse.model.Product;
import com.datapulse.model.RoleType;
import com.datapulse.model.Store;
import com.datapulse.repository.ProductAttributeRepository;
import com.datapulse.repository.ProductRepository;
import com.datapulse.repository.StoreRepository;
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
    private final LogEventPublisher logEventPublisher;

    private UserDetailsImpl getCurrentUser(Authentication auth) {
        return (UserDetailsImpl) auth.getPrincipal();
    }

    public Page<ProductResponse> getProducts(Authentication auth, Pageable pageable) {
        if (auth == null || !auth.isAuthenticated()) {
            return productRepository.findAll(pageable).map(ProductResponse::from);
        }

        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        if (role == RoleType.CORPORATE) {
            List<String> storeIds = storeRepository.findByOwnerId(currentUser.getId())
                    .stream().map(Store::getId).toList();
            return productRepository.findByStoreIdIn(storeIds, pageable).map(ProductResponse::from);
        }

        return productRepository.findAll(pageable).map(ProductResponse::from);
    }

    public Page<ProductResponse> searchProducts(String query, String categoryId, String brand, Double minPrice, Double maxPrice, Pageable pageable) {
        return productRepository.search(query, categoryId, brand, minPrice, maxPrice, pageable).map(ProductResponse::from);
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
        return ProductResponse.from(product, attrs);
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

        productRepository.delete(product);
    }
}
