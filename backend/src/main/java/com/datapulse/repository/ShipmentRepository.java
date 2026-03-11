package com.datapulse.repository;

import com.datapulse.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, String> {
    Optional<Shipment> findFirstByOrderId(String orderId);
    List<Shipment> findByOrderIdIn(List<String> orderIds);
}
