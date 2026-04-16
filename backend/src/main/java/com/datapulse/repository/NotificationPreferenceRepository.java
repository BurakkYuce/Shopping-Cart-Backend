package com.datapulse.repository;

import com.datapulse.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, String> {

    @Query("SELECT p.userId FROM NotificationPreference p WHERE p.newArrivals = true")
    List<String> findUserIdsWithNewArrivalsEnabled();

    @Query("SELECT p.userId FROM NotificationPreference p WHERE p.promotions = true")
    List<String> findUserIdsWithPromotionsEnabled();
}
