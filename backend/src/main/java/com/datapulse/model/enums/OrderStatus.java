package com.datapulse.model.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    PENDING("Beklemede"),
    PROCESSING("Hazırlanıyor"),
    SHIPPED("Kargoya Verildi"),
    DELIVERED("Teslim Edildi"),
    RETURNED("İade Edildi"),
    CANCELLED("İptal Edildi");

    private final String trLabel;

    OrderStatus(String trLabel) {
        this.trLabel = trLabel;
    }

    public String getTrLabel() {
        return trLabel;
    }

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            PENDING, EnumSet.of(PROCESSING, CANCELLED),
            PROCESSING, EnumSet.of(SHIPPED, CANCELLED),
            SHIPPED, EnumSet.of(DELIVERED),
            DELIVERED, EnumSet.of(RETURNED),
            RETURNED, EnumSet.noneOf(OrderStatus.class),
            CANCELLED, EnumSet.noneOf(OrderStatus.class)
    );

    public boolean canTransitionTo(OrderStatus next) {
        if (next == null) return false;
        return TRANSITIONS.getOrDefault(this, EnumSet.noneOf(OrderStatus.class)).contains(next);
    }

    public static OrderStatus fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return PENDING;
        }
        String normalized = raw.trim().toUpperCase().replace(" ", "_").replace("-", "_");
        return switch (normalized) {
            case "PENDING", "NEW", "CREATED" -> PENDING;
            case "PROCESSING", "CONFIRMED", "PAID" -> PROCESSING;
            case "SHIPPED", "IN_TRANSIT", "DISPATCHED" -> SHIPPED;
            case "DELIVERED", "COMPLETED" -> DELIVERED;
            case "RETURNED", "RETURN_REQUESTED", "REFUNDED" -> RETURNED;
            case "CANCELLED", "CANCELED", "VOID" -> CANCELLED;
            default -> {
                try {
                    yield OrderStatus.valueOf(normalized);
                } catch (IllegalArgumentException e) {
                    yield PENDING;
                }
            }
        };
    }
}
