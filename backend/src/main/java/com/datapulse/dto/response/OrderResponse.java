package com.datapulse.dto.response;

import com.datapulse.model.Order;
import com.datapulse.model.OrderItem;
import com.datapulse.model.Shipment;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponse {
    private String id;
    private String userId;
    private String storeId;
    private String status;
    private Double grandTotal;
    private LocalDateTime createdAt;
    private String paymentMethod;
    private List<OrderItemDetail> items;
    private String shipmentStatus;

    @Data
    public static class OrderItemDetail {
        private String id;
        private String productId;
        private Integer quantity;
        private Double price;

        public static OrderItemDetail from(OrderItem item) {
            OrderItemDetail d = new OrderItemDetail();
            d.id = item.getId();
            d.productId = item.getProductId();
            d.quantity = item.getQuantity();
            d.price = item.getPrice();
            return d;
        }
    }

    public static OrderResponse from(Order order, List<OrderItem> items, Shipment shipment) {
        OrderResponse r = new OrderResponse();
        r.id = order.getId();
        r.userId = order.getUserId();
        r.storeId = order.getStoreId();
        r.status = order.getStatus();
        r.grandTotal = order.getGrandTotal();
        r.createdAt = order.getCreatedAt();
        r.paymentMethod = order.getPaymentMethod();
        r.items = items != null ? items.stream().map(OrderItemDetail::from).toList() : List.of();
        r.shipmentStatus = shipment != null ? shipment.getStatus() : null;
        return r;
    }
}
