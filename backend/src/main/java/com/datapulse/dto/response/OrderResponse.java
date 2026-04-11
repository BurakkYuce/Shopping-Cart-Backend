package com.datapulse.dto.response;

import com.datapulse.model.Order;
import com.datapulse.model.OrderItem;
import com.datapulse.model.Product;
import com.datapulse.model.Shipment;
import com.datapulse.model.enums.OrderStatus;
import com.datapulse.model.enums.PaymentMethod;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class OrderResponse {
    private String id;
    private String userId;
    private String storeId;
    private OrderStatus status;
    private Double subtotal;
    private Double taxAmount;
    private Double grandTotal;
    private LocalDateTime createdAt;
    private PaymentMethod paymentMethod;
    private List<OrderItemDetail> items;
    private String shipmentStatus;

    @Data
    public static class OrderItemDetail {
        private String id;
        private String productId;
        private String productName;
        private String imageUrl;
        private Integer quantity;
        private Double price;

        public static OrderItemDetail from(OrderItem item, Map<String, Product> productMap) {
            OrderItemDetail d = new OrderItemDetail();
            d.id = item.getId();
            d.productId = item.getProductId();
            d.quantity = item.getQuantity();
            d.price = item.getPrice();
            Product product = productMap.get(item.getProductId());
            if (product != null) {
                d.productName = product.getName();
                d.imageUrl = product.getImageUrl();
            }
            return d;
        }
    }

    public static OrderResponse from(Order order, List<OrderItem> items, Shipment shipment, Map<String, Product> productMap) {
        OrderResponse r = new OrderResponse();
        r.id = order.getId();
        r.userId = order.getUserId();
        r.storeId = order.getStoreId();
        r.status = order.getStatus();
        r.subtotal = order.getSubtotal();
        r.taxAmount = order.getTaxAmount();
        r.grandTotal = order.getGrandTotal();
        r.createdAt = order.getCreatedAt();
        r.paymentMethod = order.getPaymentMethod();
        r.items = items != null ? items.stream().map(i -> OrderItemDetail.from(i, productMap)).toList() : List.of();
        r.shipmentStatus = shipment != null ? shipment.getStatus() : null;
        return r;
    }
}
