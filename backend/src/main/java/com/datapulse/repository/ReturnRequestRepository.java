package com.datapulse.repository;

import com.datapulse.model.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, String> {
    List<ReturnRequest> findByOrderId(String orderId);
    List<ReturnRequest> findByUserId(String userId);
}
