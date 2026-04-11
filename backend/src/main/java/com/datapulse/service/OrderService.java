package com.datapulse.service;

import com.datapulse.dto.request.CreateOrderRequest;
import com.datapulse.dto.response.OrderResponse;
import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.exception.UnauthorizedAccessException;
import com.datapulse.logging.LogEventPublisher;
import com.datapulse.logging.LogEventType;
import com.datapulse.model.Order;
import com.datapulse.model.OrderItem;
import com.datapulse.model.Product;
import com.datapulse.model.RoleType;
import com.datapulse.model.Shipment;
import com.datapulse.model.Store;
import com.datapulse.model.enums.OrderStatus;
import com.datapulse.model.enums.PaymentMethod;
import com.datapulse.repository.OrderItemRepository;
import com.datapulse.repository.OrderRepository;
import com.datapulse.repository.ProductRepository;
import com.datapulse.repository.ShipmentRepository;
import com.datapulse.repository.StoreRepository;
import com.datapulse.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.20");
    private static final int MONEY_SCALE = 2;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ShipmentRepository shipmentRepository;
    private final StoreRepository storeRepository;
    private final LogEventPublisher logEventPublisher;

    private UserDetailsImpl getCurrentUser(Authentication auth) {
        return (UserDetailsImpl) auth.getPrincipal();
    }

    private OrderResponse buildOrderResponse(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        Shipment shipment = shipmentRepository.findFirstByOrderId(order.getId()).orElse(null);
        List<String> productIds = items.stream().map(OrderItem::getProductId).distinct().toList();
        Map<String, Product> productMap = productRepository.findAllById(productIds)
                .stream().collect(java.util.stream.Collectors.toMap(Product::getId, p -> p));
        return OrderResponse.from(order, items, shipment, productMap);
    }

    private List<OrderResponse> buildBulkResponses(List<Order> orders) {
        if (orders.isEmpty()) return List.of();
        List<String> orderIds = orders.stream().map(Order::getId).toList();
        // Fetch items and shipments in bulk to avoid N+1
        List<OrderItem> allItems = orderItemRepository.findByOrderIdIn(orderIds);
        Map<String, List<OrderItem>> itemsByOrder = allItems
                .stream().collect(java.util.stream.Collectors.groupingBy(OrderItem::getOrderId));
        Map<String, Shipment> shipmentByOrder = shipmentRepository.findByOrderIdIn(orderIds)
                .stream().collect(java.util.stream.Collectors.toMap(
                        Shipment::getOrderId, s -> s, (a, b) -> a)); // keep first if duplicates
        // Bulk fetch products for all items
        List<String> productIds = allItems.stream().map(OrderItem::getProductId).distinct().toList();
        Map<String, Product> productMap = productRepository.findAllById(productIds)
                .stream().collect(java.util.stream.Collectors.toMap(Product::getId, p -> p));
        return orders.stream()
                .map(o -> OrderResponse.from(o,
                        itemsByOrder.getOrDefault(o.getId(), List.of()),
                        shipmentByOrder.get(o.getId()),
                        productMap))
                .toList();
    }

    public Page<OrderResponse> getOrders(Authentication auth, Pageable pageable) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        Page<Order> orderPage;
        if (role == RoleType.INDIVIDUAL) {
            orderPage = orderRepository.findByUserId(currentUser.getId(), pageable);
        } else if (role == RoleType.CORPORATE) {
            List<String> storeIds = storeRepository.findByOwnerId(currentUser.getId())
                    .stream().map(Store::getId).toList();
            orderPage = orderRepository.findByStoreIdIn(storeIds, pageable);
        } else {
            orderPage = orderRepository.findAll(pageable);
        }

        List<OrderResponse> responses = buildBulkResponses(orderPage.getContent());
        return new PageImpl<>(responses, pageable, orderPage.getTotalElements());
    }

    public OrderResponse getOrderById(String id, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order", id));

        if (role == RoleType.INDIVIDUAL) {
            if (!order.getUserId().equals(currentUser.getId())) {
                throw new UnauthorizedAccessException("Access denied: this order does not belong to you");
            }
        } else if (role == RoleType.CORPORATE) {
            List<String> storeIds = storeRepository.findByOwnerId(currentUser.getId())
                    .stream().map(Store::getId).toList();
            if (!storeIds.contains(order.getStoreId())) {
                throw new UnauthorizedAccessException("Access denied: this order is not from your store");
            }
        }

        return buildOrderResponse(order);
    }

    public OrderResponse createOrder(CreateOrderRequest req, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        if (role != RoleType.INDIVIDUAL && role != RoleType.ADMIN) {
            throw new UnauthorizedAccessException("Access denied: INDIVIDUAL or ADMIN role required");
        }

        String orderId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        List<Product> productsToUpdate = new ArrayList<>();
        for (CreateOrderRequest.OrderItemRequest itemReq : req.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product", itemReq.getProductId()));

            if (product.getStockQuantity() < itemReq.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for product: " + product.getName()
                        + " (available: " + product.getStockQuantity() + ", requested: " + itemReq.getQuantity() + ")");
            }

            product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());
            productsToUpdate.add(product);

            BigDecimal unitPrice = BigDecimal.valueOf(product.getUnitPrice());
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()))
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);

            String itemId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            OrderItem orderItem = new OrderItem();
            orderItem.setId(itemId);
            orderItem.setOrderId(orderId);
            orderItem.setProductId(itemReq.getProductId());
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setPrice(lineTotal.doubleValue());
            orderItems.add(orderItem);
        }

        subtotal = subtotal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal taxAmount = subtotal.multiply(TAX_RATE).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal grandTotal = subtotal.add(taxAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(currentUser.getId());
        order.setStoreId(req.getStoreId());
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(subtotal.doubleValue());
        order.setTaxAmount(taxAmount.doubleValue());
        order.setGrandTotal(grandTotal.doubleValue());
        order.setCreatedAt(LocalDateTime.now());
        order.setPaymentMethod(PaymentMethod.fromString(req.getPaymentMethod()));

        orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);
        productRepository.saveAll(productsToUpdate);

        logEventPublisher.publish(
                LogEventType.ORDER_PLACED,
                currentUser.getId(),
                role.name(),
                Map.of("orderId", orderId, "grandTotal", grandTotal.doubleValue(), "storeId", req.getStoreId())
        );

        Map<String, Product> productMap = productsToUpdate.stream()
                .collect(java.util.stream.Collectors.toMap(Product::getId, p -> p));
        return OrderResponse.from(order, orderItems, null, productMap);
    }

    public OrderResponse updateOrderStatus(String id, String status, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order", id));

        if (role == RoleType.CORPORATE) {
            List<String> storeIds = storeRepository.findByOwnerId(currentUser.getId())
                    .stream().map(Store::getId).toList();
            if (!storeIds.contains(order.getStoreId())) {
                throw new UnauthorizedAccessException("Access denied: this order is not from your store");
            }
        } else if (role != RoleType.ADMIN) {
            throw new UnauthorizedAccessException("Access denied: ADMIN or CORPORATE store owner required");
        }

        OrderStatus previousStatus = order.getStatus();
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown order status: " + status);
        }
        if (previousStatus != null && !previousStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException("Invalid status transition: "
                    + previousStatus + " -> " + newStatus);
        }
        order.setStatus(newStatus);
        orderRepository.save(order);

        logEventPublisher.publish(
                LogEventType.ORDER_STATUS_CHANGED,
                currentUser.getId(),
                role.name(),
                Map.of("orderId", id, "previousStatus", previousStatus != null ? previousStatus.name() : "", "newStatus", newStatus.name())
        );

        return buildOrderResponse(order);
    }

    public OrderResponse cancelOrder(String id, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order", id));

        if (role == RoleType.INDIVIDUAL && !order.getUserId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("Access denied: this order does not belong to you");
        }

        OrderStatus previousStatus = order.getStatus();
        if (previousStatus == null || !previousStatus.canTransitionTo(OrderStatus.CANCELLED)) {
            throw new IllegalStateException("Order cannot be cancelled from status: " + previousStatus);
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        for (OrderItem item : items) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        logEventPublisher.publish(
                LogEventType.ORDER_STATUS_CHANGED,
                currentUser.getId(),
                role.name(),
                Map.of("orderId", id, "previousStatus", previousStatus.name(), "newStatus", OrderStatus.CANCELLED.name())
        );

        return buildOrderResponse(order);
    }

    public OrderResponse returnOrder(String id, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order", id));

        if (role == RoleType.INDIVIDUAL && !order.getUserId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("Access denied: this order does not belong to you");
        }

        OrderStatus previousStatus = order.getStatus();
        if (previousStatus != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Only delivered orders can be returned. Current status: " + previousStatus);
        }

        order.setStatus(OrderStatus.RETURNED);
        orderRepository.save(order);

        logEventPublisher.publish(
                LogEventType.ORDER_STATUS_CHANGED,
                currentUser.getId(),
                role.name(),
                Map.of("orderId", id, "previousStatus", previousStatus.name(), "newStatus", OrderStatus.RETURNED.name())
        );

        return buildOrderResponse(order);
    }
}
