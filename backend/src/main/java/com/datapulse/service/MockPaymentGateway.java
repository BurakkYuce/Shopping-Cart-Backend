package com.datapulse.service;

import com.datapulse.model.enums.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class MockPaymentGateway implements PaymentService {

    private final ConcurrentHashMap<String, PaymentResult> idempotencyCache = new ConcurrentHashMap<>();

    @Value("${app.payment.mock.failure-rate:0.05}")
    private double failureRate;

    @Override
    public PaymentResult charge(String idempotencyKey,
                                BigDecimal amount,
                                PaymentMethod method,
                                String orderId,
                                String userId) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            PaymentResult cached = idempotencyCache.get(idempotencyKey);
            if (cached != null) {
                log.info("Payment cache hit for idempotency-key={} result={}", idempotencyKey, cached.success());
                return cached;
            }
        }

        PaymentResult result;
        if (method == PaymentMethod.COD) {
            result = PaymentResult.ok("cod_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        } else if (Math.random() < failureRate) {
            result = PaymentResult.failed("3DS authentication failed");
        } else {
            result = PaymentResult.ok("txn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        }

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyCache.put(idempotencyKey, result);
        }
        log.info("Mock payment charge order={} amount={} method={} success={} txn={}",
                orderId, amount, method, result.success(), result.transactionId());
        return result;
    }
}
