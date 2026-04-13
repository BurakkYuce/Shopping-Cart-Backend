package com.datapulse.service;

import com.datapulse.model.AuditLog;
import com.datapulse.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(String userId, String action, String resource, String details, String ipAddress) {
        AuditLog entry = new AuditLog();
        entry.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        entry.setUserId(userId);
        entry.setAction(action);
        entry.setResource(resource);
        entry.setDetails(details);
        entry.setIpAddress(ipAddress);
        auditLogRepository.save(entry);
    }

    public Page<AuditLog> query(String userId, String action,
                                LocalDateTime from, LocalDateTime to,
                                Pageable pageable) {
        return auditLogRepository.findFiltered(userId, action, from, to, pageable);
    }
}
