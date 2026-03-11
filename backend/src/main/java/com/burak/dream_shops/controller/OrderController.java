package com.burak.dream_shops.controller;

import com.burak.dream_shops.dto.OrderDto;
import com.burak.dream_shops.exceptions.ResourcesNotFoundException;
import com.burak.dream_shops.model.Order;
import com.burak.dream_shops.request.ApiResponse;
import com.burak.dream_shops.service.order.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Sipariş (Order) işlemlerini yöneten controller.
 * Sipariş oluşturma, tekil sipariş getirme ve kullanıcıya ait tüm siparişleri listeleme endpoint'lerini içerir.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/orders")
public class OrderController {
    private final IOrderService orderService;

    /**
     * POST /api/v1/orders/order?userId=
     * Kullanıcının mevcut sepetinden sipariş oluşturur.
     * Kullanıcının sisteme bağlı bir sepeti (cart.user_id = userId) olması zorunludur.
     * Sipariş sonrası sepet otomatik temizlenir.
     */
    @PostMapping("/order")
    public ResponseEntity<ApiResponse> createOrder(@RequestParam Long userId) {
        try {
            Order order =  orderService.placeOrder(userId);
            return ResponseEntity.ok(new ApiResponse("Item Order Success!", order));
        } catch (Exception e) {
            return  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse("Error has been detected!", e.getMessage()));
        }
    }

    /** GET /api/v1/orders/{orderId}/order → Sipariş ID'sine göre tek siparişi döner. */
    @GetMapping("/{orderId}/order")
    public ResponseEntity<ApiResponse> getOrderById(@PathVariable Long orderId) {
        try {
            OrderDto order = orderService.getOrder(orderId);
            return ResponseEntity.ok(new ApiResponse("Item Order Success!", order));
        } catch (ResourcesNotFoundException e) {
            return  ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse("Oops!", e.getMessage()));
        }
    }

    /** GET /api/v1/orders/{userId}/user-orders → Kullanıcının tüm siparişlerini listeler. */
    @GetMapping("/{userId}/user-orders")
    public ResponseEntity<ApiResponse> getUserOrders(@PathVariable Long userId) {
        try {
            List<OrderDto> order = orderService.getUserOrders(userId);
            return ResponseEntity.ok(new ApiResponse("Item Order Success!", order));
        } catch (ResourcesNotFoundException e) {
            return  ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse("Oops!", e.getMessage()));
        }
    }
}