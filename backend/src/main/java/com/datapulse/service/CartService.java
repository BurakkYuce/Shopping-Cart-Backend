package com.datapulse.service;

import com.datapulse.dto.request.AddToCartRequest;
import com.datapulse.dto.request.CreateOrderRequest;
import com.datapulse.dto.response.CartItemResponse;
import com.datapulse.dto.response.CartResponse;
import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.model.CartItem;
import com.datapulse.model.Product;
import com.datapulse.repository.CartItemRepository;
import com.datapulse.repository.ProductRepository;
import com.datapulse.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    private UserDetailsImpl getCurrentUser(Authentication auth) {
        return (UserDetailsImpl) auth.getPrincipal();
    }

    public CartResponse getCart(Authentication auth) {
        String userId = getCurrentUser(auth).getId();
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        return buildCartResponse(items);
    }

    @Transactional
    public CartResponse addToCart(AddToCartRequest request, Authentication auth) {
        String userId = getCurrentUser(auth).getId();

        productRepository.findById(request.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product", request.getProductId()));

        String newId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        cartItemRepository.upsertCartItem(newId, userId, request.getProductId(), request.getQuantity());

        return getCart(auth);
    }

    public CartResponse updateQuantity(String productId, int quantity, Authentication auth) {
        String userId = getCurrentUser(auth).getId();

        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new EntityNotFoundException("CartItem for product", productId));

        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return getCart(auth);
    }

    @Transactional
    public void removeFromCart(String productId, Authentication auth) {
        String userId = getCurrentUser(auth).getId();
        cartItemRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public void clearCart(Authentication auth) {
        String userId = getCurrentUser(auth).getId();
        cartItemRepository.deleteByUserId(userId);
    }

    public List<CreateOrderRequest> toOrderRequests(String paymentMethod, Authentication auth) {
        String userId = getCurrentUser(auth).getId();
        List<CartItem> items = cartItemRepository.findByUserId(userId);

        if (items.isEmpty()) {
            throw new EntityNotFoundException("Cart is empty", userId);
        }

        List<String> productIds = items.stream().map(CartItem::getProductId).toList();
        Map<String, Product> productMap = productRepository.findAllByIdWithStore(productIds)
                .stream().collect(Collectors.toMap(Product::getId, p -> p));

        // Group cart items by storeId
        Map<String, List<CartItem>> byStore = items.stream()
                .collect(Collectors.groupingBy(ci -> {
                    Product p = productMap.get(ci.getProductId());
                    return p != null ? p.getStoreId() : "unknown";
                }));

        return byStore.entrySet().stream().map(entry -> {
            CreateOrderRequest req = new CreateOrderRequest();
            req.setStoreId(entry.getKey());
            req.setPaymentMethod(paymentMethod);
            req.setItems(entry.getValue().stream().map(ci -> {
                CreateOrderRequest.OrderItemRequest oir = new CreateOrderRequest.OrderItemRequest();
                oir.setProductId(ci.getProductId());
                oir.setQuantity(ci.getQuantity());
                return oir;
            }).toList());
            return req;
        }).toList();
    }

    public BigDecimal calculateCartSubtotal(Authentication auth) {
        String userId = getCurrentUser(auth).getId();
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        List<String> productIds = items.stream().map(CartItem::getProductId).toList();
        Map<String, Product> productMap = productIds.isEmpty() ? Map.of() :
                productRepository.findAllById(productIds)
                        .stream().collect(Collectors.toMap(Product::getId, p -> p));
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem ci : items) {
            Product p = productMap.get(ci.getProductId());
            if (p != null) {
                subtotal = subtotal.add(BigDecimal.valueOf(p.getUnitPrice()).multiply(BigDecimal.valueOf(ci.getQuantity())));
            }
        }
        return subtotal;
    }

    public BigDecimal calculateOrderSubtotal(CreateOrderRequest req) {
        List<String> productIds = req.getItems().stream()
                .map(CreateOrderRequest.OrderItemRequest::getProductId).toList();
        Map<String, Product> productMap = productRepository.findAllById(productIds)
                .stream().collect(Collectors.toMap(Product::getId, p -> p));
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CreateOrderRequest.OrderItemRequest item : req.getItems()) {
            Product p = productMap.get(item.getProductId());
            if (p != null) {
                subtotal = subtotal.add(BigDecimal.valueOf(p.getUnitPrice()).multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        return subtotal;
    }

    private CartResponse buildCartResponse(List<CartItem> items) {
        List<String> productIds = items.stream().map(CartItem::getProductId).toList();
        Map<String, Product> productMap = productIds.isEmpty() ? Map.of() :
                productRepository.findAllByIdWithStore(productIds)
                .stream().collect(Collectors.toMap(Product::getId, p -> p));

        CartResponse response = new CartResponse();
        double totalPrice = 0.0;

        List<CartItemResponse> itemResponses = items.stream().map(ci -> {
            Product product = productMap.get(ci.getProductId());
            CartItemResponse r = new CartItemResponse();
            r.setId(ci.getId());
            r.setProductId(ci.getProductId());
            r.setProductName(product != null ? product.getName() : "Unknown");
            r.setImageUrl(product != null ? product.getImageUrl() : null);
            r.setUnitPrice(product != null ? product.getUnitPrice() : 0.0);
            r.setQuantity(ci.getQuantity());
            r.setLineTotal(r.getUnitPrice() * ci.getQuantity());
            r.setStoreId(product != null ? product.getStoreId() : null);
            r.setStoreName(product != null && product.getStore() != null ? product.getStore().getName() : "Unknown Store");
            return r;
        }).toList();

        totalPrice = itemResponses.stream().mapToDouble(CartItemResponse::getLineTotal).sum();

        response.setItems(itemResponses);
        response.setTotalItems(items.stream().mapToInt(CartItem::getQuantity).sum());
        response.setTotalPrice(totalPrice);
        return response;
    }
}
