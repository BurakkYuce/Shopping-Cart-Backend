package com.datapulse.service;

import com.datapulse.dto.request.CreateOrderRequest;
import com.datapulse.dto.response.OrderResponse;
import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.exception.InsufficientStockException;
import com.datapulse.exception.InvalidOrderStateException;
import com.datapulse.exception.PaymentFailedException;
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
import com.datapulse.model.ReturnRequest;
import com.datapulse.repository.OrderItemRepository;
import com.datapulse.repository.OrderRepository;
import com.datapulse.repository.ProductRepository;
import com.datapulse.repository.ReturnRequestRepository;
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
    private final ReturnRequestRepository returnRequestRepository;
    private final LogEventPublisher logEventPublisher;
    private final PaymentService paymentService;
    private final NotificationDispatcher notificationDispatcher;

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
        return getOrders(auth, pageable, null);
    }

    public Page<OrderResponse> getOrders(Authentication auth, Pageable pageable, String statusFilter) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        Page<Order> orderPage;
        if (role == RoleType.INDIVIDUAL) {
            orderPage = orderRepository.findByUserId(currentUser.getId(), pageable);
        } else if (role == RoleType.CORPORATE) {
            List<String> storeIds = storeRepository.findByOwnerId(currentUser.getId())
                    .stream().map(Store::getId).toList();
            // Sellers need to see shipping-ready / confirmation-pending orders first so nothing
            // slips through the cracks. Strip the client-provided sort and use our priority query.
            Pageable priorityPageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(), pageable.getPageSize());
            orderPage = storeIds.isEmpty()
                    ? Page.empty(priorityPageable)
                    : orderRepository.findSellerOrdersByPriority(storeIds, priorityPageable);
        } else {
            orderPage = orderRepository.findAll(pageable);
        }

        List<Order> filtered = filterByStatus(orderPage.getContent(), statusFilter);
        List<OrderResponse> responses = buildBulkResponses(filtered);
        long total = statusFilter == null || statusFilter.isBlank()
                ? orderPage.getTotalElements()
                : filtered.size();
        return new PageImpl<>(responses, orderPage.getPageable(), total);
    }

    /** Parse status query — accepts a single status ("delivered"), a comma-separated list
     *  ("pending,processing,shipped"), or the alias "active" meaning in-flight orders. */
    private List<Order> filterByStatus(List<Order> orders, String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank()) return orders;
        java.util.Set<OrderStatus> wanted = new java.util.HashSet<>();
        for (String raw : statusFilter.split(",")) {
            String token = raw.trim();
            if (token.isEmpty()) continue;
            if ("active".equalsIgnoreCase(token)) {
                wanted.add(OrderStatus.PENDING);
                wanted.add(OrderStatus.PROCESSING);
                wanted.add(OrderStatus.SHIPPED);
            } else {
                try {
                    wanted.add(OrderStatus.fromString(token));
                } catch (IllegalArgumentException ignored) { /* silently drop unknown values */ }
            }
        }
        if (wanted.isEmpty()) return orders;
        return orders.stream().filter(o -> wanted.contains(o.getStatus())).toList();
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

    public OrderResponse createOrder(CreateOrderRequest req, String idempotencyKey, Authentication auth) {
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
                throw new InsufficientStockException("Insufficient stock for product: " + product.getName()
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
        BigDecimal discountAmount = req.getDiscountAmount() != null
                ? BigDecimal.valueOf(req.getDiscountAmount()).setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal taxableAmount = subtotal.subtract(discountAmount);
        if (taxableAmount.compareTo(BigDecimal.ZERO) < 0) taxableAmount = BigDecimal.ZERO;
        BigDecimal taxAmount = taxableAmount.multiply(TAX_RATE).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal grandTotal = taxableAmount.add(taxAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        PaymentMethod paymentMethod = PaymentMethod.fromString(req.getPaymentMethod());
        PaymentService.PaymentResult paymentResult = paymentService.charge(
                idempotencyKey, grandTotal, paymentMethod, orderId, currentUser.getId());
        if (!paymentResult.success()) {
            throw new PaymentFailedException(paymentResult.failureReason());
        }

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(currentUser.getId());
        order.setStoreId(req.getStoreId());
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(subtotal.doubleValue());
        order.setCouponCode(req.getCouponCode());
        order.setDiscountAmount(discountAmount.doubleValue());
        order.setTaxAmount(taxAmount.doubleValue());
        order.setGrandTotal(grandTotal.doubleValue());
        order.setCreatedAt(LocalDateTime.now());
        order.setPaymentMethod(paymentMethod);

        orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);
        productRepository.saveAll(productsToUpdate);

        logEventPublisher.publish(
                LogEventType.ORDER_PLACED,
                currentUser.getId(),
                role.name(),
                Map.of("orderId", orderId,
                        "grandTotal", grandTotal.doubleValue(),
                        "storeId", req.getStoreId(),
                        "transactionId", paymentResult.transactionId())
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
            throw new InvalidOrderStateException("Invalid status transition: "
                    + previousStatus + " -> " + newStatus);
        }
        order.setStatus(newStatus);
        if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
            order.setReturnDeadline(LocalDateTime.now().plusDays(14));
        }
        orderRepository.save(order);

        logEventPublisher.publish(
                LogEventType.ORDER_STATUS_CHANGED,
                currentUser.getId(),
                role.name(),
                Map.of("orderId", id, "previousStatus", previousStatus != null ? previousStatus.name() : "", "newStatus", newStatus.name())
        );

        if (previousStatus != newStatus) {
            notificationDispatcher.dispatchOrderStatus(order.getUserId(), order.getId(), newStatus.name());
        }

        return buildOrderResponse(order);
    }

    public OrderResponse cancelOrder(String id, String reason, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order", id));

        if (role == RoleType.INDIVIDUAL && !order.getUserId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("Access denied: this order does not belong to you");
        }

        OrderStatus previousStatus = order.getStatus();
        if (previousStatus == null || !previousStatus.canTransitionTo(OrderStatus.CANCELLED)) {
            throw new InvalidOrderStateException("Order cannot be cancelled from status: " + previousStatus);
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
        order.setCancellationReason(reason);
        order.setRefundedAt(LocalDateTime.now());
        orderRepository.save(order);

        logEventPublisher.publish(
                LogEventType.ORDER_STATUS_CHANGED,
                currentUser.getId(),
                role.name(),
                Map.of("orderId", id, "previousStatus", previousStatus.name(), "newStatus", OrderStatus.CANCELLED.name())
        );

        notificationDispatcher.dispatchOrderStatus(order.getUserId(), order.getId(), OrderStatus.CANCELLED.name());

        return buildOrderResponse(order);
    }

    public ReturnRequest createReturnRequest(String orderId, String reason, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));

        if (!order.getUserId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("Access denied: this order does not belong to you");
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException("Only delivered orders can be returned. Current status: " + order.getStatus());
        }

        if (order.getReturnDeadline() != null && LocalDateTime.now().isAfter(order.getReturnDeadline())) {
            throw new InvalidOrderStateException("Return window has expired (14 days after delivery)");
        }
        if (order.getDeliveredAt() != null && order.getReturnDeadline() == null
                && LocalDateTime.now().isAfter(order.getDeliveredAt().plusDays(14))) {
            throw new InvalidOrderStateException("Return window has expired (14 days after delivery)");
        }

        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        ReturnRequest rr = new ReturnRequest();
        rr.setId(requestId);
        rr.setOrderId(orderId);
        rr.setUserId(currentUser.getId());
        rr.setReason(reason);
        rr.setStatus("PENDING");
        rr.setCreatedAt(LocalDateTime.now());
        returnRequestRepository.save(rr);

        order.setStatus(OrderStatus.RETURNED);
        order.setRefundedAt(LocalDateTime.now());
        orderRepository.save(order);

        logEventPublisher.publish(
                LogEventType.ORDER_STATUS_CHANGED,
                currentUser.getId(),
                currentUser.getRole().name(),
                Map.of("orderId", orderId, "returnRequestId", requestId, "previousStatus", "DELIVERED", "newStatus", "RETURNED")
        );

        return rr;
    }

    public OrderResponse getTracking(String orderId, Authentication auth) {
        return getOrderById(orderId, auth);
    }
}
