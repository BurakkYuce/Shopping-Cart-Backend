package com.datapulse.service;

import com.datapulse.dto.response.AnalyticsCustomerResponse;
import com.datapulse.dto.response.AnalyticsSalesResponse;
import com.datapulse.exception.UnauthorizedAccessException;
import com.datapulse.model.CustomerProfile;
import com.datapulse.model.Order;
import com.datapulse.model.RoleType;
import com.datapulse.model.User;
import com.datapulse.repository.CategoryRepository;
import com.datapulse.repository.CustomerProfileRepository;
import com.datapulse.repository.OrderItemRepository;
import com.datapulse.repository.OrderRepository;
import com.datapulse.repository.ProductRepository;
import com.datapulse.repository.ReviewRepository;
import com.datapulse.repository.StoreRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private Authentication buildAdminAuth() {
        User user = new User("admin1", "admin@example.com", "hashed", RoleType.ADMIN, null);
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        return auth;
    }

    private Authentication buildIndividualAuth() {
        User user = new User("user1", "user@example.com", "hashed", RoleType.INDIVIDUAL, "female");
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        return auth;
    }

    @Test
    void getSalesAnalytics_admin_returnsAllOrders() {
        Authentication auth = buildAdminAuth();

        LocalDateTime now = LocalDateTime.now();
        Order o1 = new Order("ord1", "user1", null, "store1", null, "completed", 100.0, now, "card");
        Order o2 = new Order("ord2", "user2", null, "store1", null, "completed", 200.0, now, "card");
        Order o3 = new Order("ord3", "user3", null, "store1", null, "completed", 300.0, now, "cash");

        when(orderRepository.findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(o1, o2, o3));

        AnalyticsSalesResponse response = analyticsService.getSalesAnalytics(null, null, auth);

        assertNotNull(response);
        assertEquals(600.0, response.getTotalRevenue());
        assertEquals(3L, response.getOrderCount());
        assertEquals(200.0, response.getAverageOrderValue());
    }

    @Test
    void getCustomerAnalytics_notAdmin_throwsUnauthorized() {
        Authentication auth = buildIndividualAuth();

        assertThrows(UnauthorizedAccessException.class,
                () -> analyticsService.getCustomerAnalytics(auth));
    }

    @Test
    void getCustomerAnalytics_admin_returnsData() {
        Authentication auth = buildAdminAuth();

        CustomerProfile profile1 = new CustomerProfile("p1", "user1", null, 30, "Istanbul", "Gold", 500.0, 5, 4.5, "High");
        CustomerProfile profile2 = new CustomerProfile("p2", "user2", null, 40, "Istanbul", "Gold", 500.0, 10, 3.8, "Medium");

        when(customerProfileRepository.findAll()).thenReturn(List.of(profile1, profile2));

        AnalyticsCustomerResponse response = analyticsService.getCustomerAnalytics(auth);

        assertNotNull(response);
        assertEquals(35.0, response.getAverageAge());
        assertTrue(response.getSpendByMembership().containsKey("Gold"));
    }
}
