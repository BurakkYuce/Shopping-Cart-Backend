package com.datapulse.service;

import com.datapulse.dto.response.ProductResponse;
import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.model.Product;
import com.datapulse.model.WishlistItem;
import com.datapulse.repository.ProductRepository;
import com.datapulse.repository.WishlistItemRepository;
import com.datapulse.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;

    private String getUserId(Authentication auth) {
        return ((UserDetailsImpl) auth.getPrincipal()).getId();
    }

    public List<ProductResponse> getWishlist(Authentication auth) {
        String userId = getUserId(auth);
        List<WishlistItem> items = wishlistItemRepository.findByUserId(userId);
        List<String> productIds = items.stream().map(WishlistItem::getProductId).toList();
        Map<String, Product> productMap = productRepository.findAllById(productIds)
                .stream().collect(Collectors.toMap(Product::getId, p -> p));

        return items.stream()
                .map(wi -> productMap.get(wi.getProductId()))
                .filter(p -> p != null)
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse addToWishlist(String productId, Authentication auth) {
        String userId = getUserId(auth);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product", productId));

        if (wishlistItemRepository.findByUserIdAndProductId(userId, productId).isEmpty()) {
            WishlistItem item = new WishlistItem();
            item.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 8));
            item.setUserId(userId);
            item.setProductId(productId);
            wishlistItemRepository.save(item);
        }

        return ProductResponse.from(product);
    }

    @Transactional
    public void removeFromWishlist(String productId, Authentication auth) {
        String userId = getUserId(auth);
        wishlistItemRepository.deleteByUserIdAndProductId(userId, productId);
    }
}
