package com.datapulse.service;

import com.datapulse.dto.request.CreateOrderRequest;
import com.datapulse.dto.response.OrderResponse;
import com.datapulse.exception.UnauthorizedAccessException;
import com.datapulse.logging.LogEventPublisher;
import com.datapulse.model.Order;
import com.datapulse.model.Product;
import com.datapulse.model.RoleType;
import com.datapulse.model.User;
import com.datapulse.model.enums.OrderStatus;
import com.datapulse.model.enums.PaymentMethod;
import com.datapulse.repository.OrderItemRepository;
import com.datapulse.repository.OrderRepository;
import com.datapulse.repository.ProductRepository;
import com.datapulse.repository.ShipmentRepository;
import com.datapulse.repository.StoreRepository;
import com.datapulse.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private OrderService orderService;

    private Authentication buildIndividualAuth(String userId) {
        User user = new User(userId, "user@example.com", "hashed", RoleType.INDIVIDUAL, "male");
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        return auth;
    }

    private Authentication buildAdminAuth() {
        User user = new User("admin1", "admin@example.com", "hashed", RoleType.ADMIN, null);
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        return auth;
    }

    @Test
    void getOrders_individual_returnsOnlyOwnOrders() {
        Pageable pageable = PageRequest.of(0, 50);
        Authentication auth = buildIndividualAuth("user1");

        Order order1 = buildOrder("order1", "user1", "store1", OrderStatus.PENDING, 100.0, PaymentMethod.CREDIT_CARD);
        Order order2 = buildOrder("order2", "user1", "store1", OrderStatus.SHIPPED, 200.0, PaymentMethod.COD);
        List<Order> orders = List.of(order1, order2);
        Page<Order> orderPage = new PageImpl<>(orders, pageable, orders.size());

        when(orderRepository.findByUserId("user1", pageable)).thenReturn(orderPage);
        when(orderItemRepository.findByOrderIdIn(anyList())).thenReturn(Collections.emptyList());
        when(shipmentRepository.findByOrderIdIn(anyList())).thenReturn(Collections.emptyList());

        Page<OrderResponse> result = orderService.getOrders(auth, pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void createOrder_success() {
        Authentication auth = buildIndividualAuth("user1");

        Product product = new Product("prod1", "store1", null, "cat1", null, "SKU-01", "Widget", 10.0, "A widget", null, 100, null, null, null, null);

        CreateOrderRequest req = new CreateOrderRequest();
        req.setStoreId("store1");
        req.setPaymentMethod("card");

        CreateOrderRequest.OrderItemRequest itemReq = new CreateOrderRequest.OrderItemRequest();
        itemReq.setProductId("prod1");
        itemReq.setQuantity(2);
        req.setItems(List.of(itemReq));

        when(productRepository.findById("prod1")).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.createOrder(req, auth);

        assertNotNull(response);
        // subtotal 20.0 + 20% KDV = 24.0
        assertEquals(24.0, response.getGrandTotal());
        assertEquals(20.0, response.getSubtotal());
        assertEquals(4.0, response.getTaxAmount());
    }

    private Order buildOrder(String id, String userId, String storeId, OrderStatus status,
                             double grandTotal, PaymentMethod paymentMethod) {
        Order o = new Order();
        o.setId(id);
        o.setUserId(userId);
        o.setStoreId(storeId);
        o.setStatus(status);
        o.setGrandTotal(grandTotal);
        o.setCreatedAt(LocalDateTime.now());
        o.setPaymentMethod(paymentMethod);
        return o;
    }

    @Test
    void getOrderById_wrongUser_throwsUnauthorized() {
        Authentication auth = buildIndividualAuth("user1");

        Order order = buildOrder("order1", "other-user", "store1", OrderStatus.PENDING, 50.0, PaymentMethod.CREDIT_CARD);

        when(orderRepository.findById("order1")).thenReturn(Optional.of(order));

        assertThrows(UnauthorizedAccessException.class,
                () -> orderService.getOrderById("order1", auth));
    }
}
