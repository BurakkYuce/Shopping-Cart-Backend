package com.datapulse.repository;

import com.datapulse.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, String> {
    List<Complaint> findByOrderId(String orderId);
    List<Complaint> findByUserId(String userId);
}
