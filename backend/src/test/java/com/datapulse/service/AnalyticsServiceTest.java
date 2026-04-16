package com.datapulse.service;

import com.datapulse.dto.response.AnalyticsCustomerResponse;
import com.datapulse.dto.response.AnalyticsSalesResponse;
import com.datapulse.model.Address;
import com.datapulse.model.Order;
import com.datapulse.model.Review;
import com.datapulse.model.RoleType;
import com.datapulse.model.User;
import com.datapulse.model.enums.OrderStatus;
import com.datapulse.model.enums.PaymentMethod;
import com.datapulse.repository.AddressRepository;
import com.datapulse.repository.CategoryRepository;
import com.datapulse.repository.OrderItemRepository;
import com.datapulse.repository.OrderRepository;
import com.datapulse.repository.ProductRepository;
import com.datapulse.repository.ReviewRepository;
import com.datapulse.repository.StoreRepository;
import com.datapulse.repository.UserRepository;
import com.datapulse.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private Authentication buildAdminAuth() {
        User user = new User();
        user.setId("admin1");
        user.setEmail("admin@example.com");
        user.setPasswordHash("hashed");
        user.setRoleType(RoleType.ADMIN);
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        return auth;
    }

    private Authentication buildIndividualAuth() {
        User user = new User();
        user.setId("user1");
        user.setEmail("user@example.com");
        user.setPasswordHash("hashed");
        user.setRoleType(RoleType.INDIVIDUAL);
        user.setGender("female");
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        return auth;
    }

    @Test
    void getSalesAnalytics_admin_returnsAllOrders() {
        Authentication auth = buildAdminAuth();

        LocalDateTime now = LocalDateTime.now();
        Order o1 = buildOrder("ord1", "user1", "store1", OrderStatus.DELIVERED, 100.0, PaymentMethod.CREDIT_CARD, now);
        Order o2 = buildOrder("ord2", "user2", "store1", OrderStatus.DELIVERED, 200.0, PaymentMethod.CREDIT_CARD, now);
        Order o3 = buildOrder("ord3", "user3", "store1", OrderStatus.DELIVERED, 300.0, PaymentMethod.COD, now);

        when(orderRepository.findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(o1, o2, o3));

        AnalyticsSalesResponse response = analyticsService.getSalesAnalytics(null, null, auth);

        assertNotNull(response);
        assertEquals(600.0, response.getTotalRevenue());
        assertEquals(3L, response.getOrderCount());
        assertEquals(200.0, response.getAverageOrderValue());
    }

    @Test
    void getCustomerAnalytics_individual_returnsSelfScopedData() {
        Authentication auth = buildIndividualAuth();

        LocalDateTime now = LocalDateTime.now();
        Order ord = buildOrder("ord1", "user1", "store1", OrderStatus.DELIVERED, 1500.0, PaymentMethod.CREDIT_CARD, now);
        when(orderRepository.findByUserIdIn(List.of("user1"))).thenReturn(List.of(ord));

        Review r = new Review();
        r.setId("rev1");
        r.setUserId("user1");
        r.setStarRating(5);
        when(reviewRepository.findByUserIdIn(List.of("user1"))).thenReturn(List.of(r));

        Address addr = new Address();
        addr.setId("addr1");
        addr.setUserId("user1");
        addr.setCity("Istanbul");
        addr.setIsDefault(true);
        when(addressRepository.findByUserIdIn(List.of("user1"))).thenReturn(List.of(addr));

        AnalyticsCustomerResponse response = analyticsService.getCustomerAnalytics(auth);

        assertNotNull(response);
        assertTrue(response.getSpendByMembership().containsKey("Silver"));
        assertTrue(response.getSatisfactionDistribution().containsKey("Satisfied"));
        assertTrue(response.getTopCities().containsKey("Istanbul"));
    }

    private Order buildOrder(String id, String userId, String storeId, OrderStatus status,
                             double grandTotal, PaymentMethod paymentMethod, LocalDateTime createdAt) {
        Order o = new Order();
        o.setId(id);
        o.setUserId(userId);
        o.setStoreId(storeId);
        o.setStatus(status);
        o.setGrandTotal(grandTotal);
        o.setCreatedAt(createdAt);
        o.setPaymentMethod(paymentMethod);
        return o;
    }
}
