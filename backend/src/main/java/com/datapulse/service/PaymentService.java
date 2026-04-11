package com.datapulse.service;

import com.datapulse.model.enums.PaymentMethod;

import java.math.BigDecimal;

public interface PaymentService {

    PaymentResult charge(String idempotencyKey,
                         BigDecimal amount,
                         PaymentMethod method,
                         String orderId,
                         String userId);

    record PaymentResult(
            boolean success,
            String transactionId,
            String failureReason
    ) {
        public static PaymentResult ok(String transactionId) {
            return new PaymentResult(true, transactionId, null);
        }

        public static PaymentResult failed(String reason) {
            return new PaymentResult(false, null, reason);
        }
    }
}
