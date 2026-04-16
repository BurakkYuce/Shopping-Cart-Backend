package com.datapulse.service;

import com.datapulse.dto.request.UpdateNotificationPreferenceRequest;
import com.datapulse.model.NotificationPreference;
import com.datapulse.repository.NotificationPreferenceRepository;
import com.datapulse.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository repository;

    @Transactional
    public NotificationPreference getOrCreate(String userId) {
        return repository.findById(userId).orElseGet(() -> {
            NotificationPreference fresh = new NotificationPreference();
            fresh.setUserId(userId);
            return repository.save(fresh);
        });
    }

    @Transactional
    public NotificationPreference update(Authentication auth, UpdateNotificationPreferenceRequest req) {
        String userId = ((UserDetailsImpl) auth.getPrincipal()).getId();
        NotificationPreference pref = getOrCreate(userId);
        if (req.getOrderUpdates() != null) pref.setOrderUpdates(req.getOrderUpdates());
        if (req.getNewArrivals() != null) pref.setNewArrivals(req.getNewArrivals());
        if (req.getPromotions() != null) pref.setPromotions(req.getPromotions());
        if (req.getNewsletter() != null) pref.setNewsletter(req.getNewsletter());
        if (req.getNewOrderSeller() != null) pref.setNewOrderSeller(req.getNewOrderSeller());
        if (req.getLowStockAlert() != null) pref.setLowStockAlert(req.getLowStockAlert());
        if (req.getNewReviewAlert() != null) pref.setNewReviewAlert(req.getNewReviewAlert());
        if (req.getWeeklyStoreDigest() != null) pref.setWeeklyStoreDigest(req.getWeeklyStoreDigest());
        return repository.save(pref);
    }
}
