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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

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
        double grandTotal = 0.0;

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

            double itemPrice = product.getUnitPrice() * itemReq.getQuantity();
            grandTotal += itemPrice;

            String itemId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            OrderItem orderItem = new OrderItem();
            orderItem.setId(itemId);
            orderItem.setOrderId(orderId);
            orderItem.setProductId(itemReq.getProductId());
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setPrice(itemPrice);
            orderItems.add(orderItem);
        }

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(currentUser.getId());
        order.setStoreId(req.getStoreId());
        order.setStatus("pending");
        order.setGrandTotal(grandTotal);
        order.setCreatedAt(LocalDateTime.now());
        order.setPaymentMethod(req.getPaymentMethod());

        orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);
        productRepository.saveAll(productsToUpdate);

        logEventPublisher.publish(
                LogEventType.ORDER_PLACED,
                currentUser.getId(),
                role.name(),
                Map.of("orderId", orderId, "grandTotal", grandTotal, "storeId", req.getStoreId())
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

        String previousStatus = order.getStatus();
        order.setStatus(status);
        orderRepository.save(order);

        logEventPublisher.publish(
                LogEventType.ORDER_STATUS_CHANGED,
                currentUser.getId(),
                role.name(),
                Map.of("orderId", id, "previousStatus", previousStatus != null ? previousStatus : "", "newStatus", status)
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

        if (!"pending".equals(order.getStatus())) {
            throw new IllegalStateException("Only pending orders can be cancelled. Current status: " + order.getStatus());
        }

        // Restore stock
        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        for (OrderItem item : items) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }
        }

        order.setStatus("cancelled");
        orderRepository.save(order);

        logEventPublisher.publish(
                LogEventType.ORDER_STATUS_CHANGED,
                currentUser.getId(),
                role.name(),
                Map.of("orderId", id, "previousStatus", "pending", "newStatus", "cancelled")
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

        if (!"shipped".equals(order.getStatus()) && !"delivered".equals(order.getStatus())) {
            throw new IllegalStateException("Only shipped or delivered orders can be returned. Current status: " + order.getStatus());
        }

        order.setStatus("return_requested");
        orderRepository.save(order);

        logEventPublisher.publish(
                LogEventType.ORDER_STATUS_CHANGED,
                currentUser.getId(),
                role.name(),
                Map.of("orderId", id, "previousStatus", order.getStatus(), "newStatus", "return_requested")
        );

        return buildOrderResponse(order);
    }
}
