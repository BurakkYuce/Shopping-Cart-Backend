package com.datapulse.controller;

import com.datapulse.dto.response.StoreResponse;
import com.datapulse.model.AuditLog;
import com.datapulse.model.SystemSetting;
import com.datapulse.model.User;
import com.datapulse.model.enums.AccountStatus;
import com.datapulse.model.enums.StoreStatus;
import com.datapulse.security.UserDetailsImpl;
import com.datapulse.service.AuditLogService;
import com.datapulse.service.SqlExecutionService;
import com.datapulse.service.StoreService;
import com.datapulse.service.SystemSettingService;
import com.datapulse.service.UserAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final SqlExecutionService sqlExecutionService;
    private final UserAdminService userAdminService;
    private final StoreService storeService;
    private final AuditLogService auditLogService;
    private final SystemSettingService systemSettingService;

    @PostMapping("/sql/execute")
    public ResponseEntity<SqlExecutionService.SqlExecutionResult> executeSql(
            @Valid @RequestBody ExecuteSqlRequest request,
            Authentication auth) {
        UserDetailsImpl currentUser = (UserDetailsImpl) auth.getPrincipal();
        String userRole = auth.getAuthorities().iterator().next().getAuthority();
        return ResponseEntity.ok(
                sqlExecutionService.execute(request.getSql(), currentUser.getId(), userRole));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<User> updateUserStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication auth) {
        UserDetailsImpl currentUser = (UserDetailsImpl) auth.getPrincipal();
        return ResponseEntity.ok(
                userAdminService.updateAccountStatus(id, request.getStatus(), currentUser.getId()));
    }

    @PostMapping("/stores/{id}/approve")
    public ResponseEntity<StoreResponse> approveStore(@PathVariable String id, Authentication auth) {
        UserDetailsImpl currentUser = (UserDetailsImpl) auth.getPrincipal();
        return ResponseEntity.ok(storeService.updateStatus(id, StoreStatus.ACTIVE, currentUser.getId()));
    }

    @PostMapping("/stores/{id}/suspend")
    public ResponseEntity<StoreResponse> suspendStore(@PathVariable String id, Authentication auth) {
        UserDetailsImpl currentUser = (UserDetailsImpl) auth.getPrincipal();
        return ResponseEntity.ok(storeService.updateStatus(id, StoreStatus.SUSPENDED, currentUser.getId()));
    }

    @PostMapping("/stores/{id}/close")
    public ResponseEntity<StoreResponse> closeStore(@PathVariable String id, Authentication auth) {
        UserDetailsImpl currentUser = (UserDetailsImpl) auth.getPrincipal();
        return ResponseEntity.ok(storeService.updateStatus(id, StoreStatus.CLOSED, currentUser.getId()));
    }

    // ──── Audit Logs ────

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(
                auditLogService.query(userId, action, from, to, PageRequest.of(page, size)));
    }

    // ──── System Settings ────

    @GetMapping("/settings")
    public ResponseEntity<java.util.List<SystemSetting>> getSettings() {
        return ResponseEntity.ok(systemSettingService.getAll());
    }

    @PatchMapping("/settings/{key}")
    public ResponseEntity<SystemSetting> updateSetting(
            @PathVariable String key,
            @Valid @RequestBody UpdateSettingRequest request,
            Authentication auth) {
        UserDetailsImpl currentUser = (UserDetailsImpl) auth.getPrincipal();
        SystemSetting updated = systemSettingService.update(key, request.getValue());
        auditLogService.log(currentUser.getId(), "SETTING_UPDATED", key,
                "value=" + request.getValue(), null);
        return ResponseEntity.ok(updated);
    }

    @Data
    public static class ExecuteSqlRequest {
        @NotBlank
        private String sql;
    }

    @Data
    public static class UpdateUserStatusRequest {
        @NotNull
        private AccountStatus status;
    }

    @Data
    public static class UpdateSettingRequest {
        @NotBlank
        private String value;
    }
}
