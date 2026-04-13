package com.datapulse.controller;

import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.model.Complaint;
import com.datapulse.model.Order;
import com.datapulse.repository.ComplaintRepository;
import com.datapulse.repository.OrderRepository;
import com.datapulse.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Slf4j
public class SupportController {

    private final ComplaintRepository complaintRepository;
    private final OrderRepository orderRepository;

    @PostMapping("/complaints")
    public ResponseEntity<?> createComplaint(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
        String orderId = body.get("orderId");
        String subject = body.get("subject");
        String message = body.get("message");

        if (orderId != null) {
            orderRepository.findById(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("Order", orderId));
        }

        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Complaint complaint = new Complaint();
        complaint.setId(id);
        complaint.setOrderId(orderId);
        complaint.setUserId(user.getId());
        complaint.setSubject(subject != null ? subject : "General complaint");
        complaint.setMessage(message != null ? message : "");
        complaint.setStatus("OPEN");
        complaint.setCreatedAt(LocalDateTime.now());
        complaintRepository.save(complaint);

        return ResponseEntity.status(HttpStatus.CREATED).body(complaint);
    }

    @PostMapping("/handoff")
    public ResponseEntity<?> handoff(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
        String ticketId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String reason = body != null ? body.getOrDefault("reason", "Customer requested agent") : "Customer requested agent";

        log.info("Support handoff: ticketId={}, userId={}, reason={}", ticketId, user.getId(), reason);

        return ResponseEntity.ok(Map.of(
                "ticketId", ticketId,
                "status", "ESCALATED",
                "message", "Your request has been escalated to a support agent. Ticket ID: " + ticketId));
    }
}
