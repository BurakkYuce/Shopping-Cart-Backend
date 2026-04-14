package com.datapulse.service;

import com.datapulse.dto.request.CreateCouponRequest;
import com.datapulse.dto.response.CouponResponse;
import com.datapulse.dto.response.CouponValidationResponse;
import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.model.Coupon;
import com.datapulse.model.CouponUsage;
import com.datapulse.repository.CouponRepository;
import com.datapulse.repository.CouponUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponService {

    private static final int MONEY_SCALE = 2;

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;

    public CouponValidationResponse validateCoupon(String code, String userId, BigDecimal subtotal) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new EntityNotFoundException("Coupon", code));

        if (!coupon.getActive()) {
            throw new IllegalArgumentException("Coupon is inactive");
        }

        LocalDateTime now = LocalDateTime.now();
        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
            throw new IllegalArgumentException("Coupon is not yet valid");
        }
        if (coupon.getValidTo() != null && now.isAfter(coupon.getValidTo())) {
            throw new IllegalArgumentException("Coupon has expired");
        }

        if (coupon.getMaxUses() != null && coupon.getCurrentUses() >= coupon.getMaxUses()) {
            throw new IllegalArgumentException("Coupon usage limit reached");
        }

        if (couponUsageRepository.existsByCouponIdAndUserId(coupon.getId(), userId)) {
            throw new IllegalArgumentException("You have already used this coupon");
        }

        BigDecimal minOrder = BigDecimal.valueOf(coupon.getMinOrderAmount() != null ? coupon.getMinOrderAmount() : 0);
        if (subtotal.compareTo(minOrder) < 0) {
            throw new IllegalArgumentException("Minimum order amount is $" + minOrder.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        }

        BigDecimal discount = calculateDiscount(coupon, subtotal);

        CouponValidationResponse resp = new CouponValidationResponse();
        resp.setValid(true);
        resp.setCode(coupon.getCode());
        resp.setType(coupon.getType());
        resp.setDiscountAmount(discount.doubleValue());
        resp.setDescription(coupon.getDescription());
        resp.setMessage("Coupon applied successfully");
        return resp;
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
        BigDecimal discount;
        if ("PERCENTAGE".equals(coupon.getType())) {
            discount = subtotal.multiply(BigDecimal.valueOf(coupon.getValue()))
                    .divide(BigDecimal.valueOf(100), MONEY_SCALE, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscount() != null) {
                BigDecimal cap = BigDecimal.valueOf(coupon.getMaxDiscount());
                if (discount.compareTo(cap) > 0) {
                    discount = cap;
                }
            }
        } else {
            // FIXED_AMOUNT
            discount = BigDecimal.valueOf(coupon.getValue()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        // Discount cannot exceed subtotal
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }
        return discount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    @Transactional
    public void applyCoupon(String couponCode, String userId, String orderId) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(couponCode.trim())
                .orElse(null);
        if (coupon == null) return;

        String usageId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        CouponUsage usage = new CouponUsage();
        usage.setId(usageId);
        usage.setCouponId(coupon.getId());
        usage.setUserId(userId);
        usage.setOrderId(orderId);
        usage.setUsedAt(LocalDateTime.now());
        couponUsageRepository.save(usage);

        coupon.setCurrentUses(coupon.getCurrentUses() + 1);
        couponRepository.save(coupon);
    }

    // --- Admin CRUD ---

    public List<CouponResponse> listCoupons() {
        return couponRepository.findAll().stream()
                .map(CouponResponse::from)
                .toList();
    }

    public CouponResponse createCoupon(CreateCouponRequest req) {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        Coupon coupon = new Coupon();
        coupon.setId(id);
        coupon.setCode(req.getCode().trim().toUpperCase());
        coupon.setType(req.getType());
        coupon.setValue(req.getValue());
        coupon.setDescription(req.getDescription());
        coupon.setMinOrderAmount(req.getMinOrderAmount() != null ? req.getMinOrderAmount() : 0.0);
        coupon.setMaxDiscount(req.getMaxDiscount());
        coupon.setMaxUses(req.getMaxUses());
        coupon.setCurrentUses(0);
        coupon.setValidFrom(req.getValidFrom());
        coupon.setValidTo(req.getValidTo());
        coupon.setActive(true);
        coupon.setCreatedAt(LocalDateTime.now());

        couponRepository.save(coupon);
        return CouponResponse.from(coupon);
    }

    public CouponResponse updateCoupon(String id, CreateCouponRequest req) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Coupon", id));

        if (req.getCode() != null) coupon.setCode(req.getCode().trim().toUpperCase());
        if (req.getType() != null) coupon.setType(req.getType());
        if (req.getValue() != null) coupon.setValue(req.getValue());
        if (req.getDescription() != null) coupon.setDescription(req.getDescription());
        if (req.getMinOrderAmount() != null) coupon.setMinOrderAmount(req.getMinOrderAmount());
        if (req.getMaxDiscount() != null) coupon.setMaxDiscount(req.getMaxDiscount());
        if (req.getMaxUses() != null) coupon.setMaxUses(req.getMaxUses());
        if (req.getValidFrom() != null) coupon.setValidFrom(req.getValidFrom());
        if (req.getValidTo() != null) coupon.setValidTo(req.getValidTo());

        couponRepository.save(coupon);
        return CouponResponse.from(coupon);
    }

    public void deactivateCoupon(String id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Coupon", id));
        coupon.setActive(false);
        couponRepository.save(coupon);
    }
}
