package com.bpdb.dms.controller;

import com.bpdb.dms.entity.AuditLog;
import com.bpdb.dms.entity.AuditStatus;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for audit log operations
 */
@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class AuditLogController {
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    /**
     * Get audit logs with pagination and filtering
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long userId) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<AuditLog> auditLogs;
        
        // Apply filters
        if (action != null && !action.isEmpty()) {
            auditLogs = auditLogRepository.findByAction(action, pageable);
        } else if (resourceType != null && !resourceType.isEmpty()) {
            auditLogs = auditLogRepository.findByResourceType(resourceType, pageable);
        } else if (status != null && !status.isEmpty()) {
            AuditStatus auditStatus = AuditStatus.valueOf(status.toUpperCase());
            auditLogs = auditLogRepository.findByStatus(auditStatus, pageable);
        } else if (userId != null) {
            User user = new User();
            user.setId(userId);
            auditLogs = auditLogRepository.findByUser(user, pageable);
        } else if (startDate != null && endDate != null) {
            LocalDateTime start = LocalDateTime.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime end = LocalDateTime.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            auditLogs = auditLogRepository.findByDateRange(start, end, pageable);
        } else {
            auditLogs = auditLogRepository.findRecentAuditLogs(pageable);
        }
        
        Page<AuditLogResponse> responses = auditLogs.map(this::createAuditLogResponse);
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Get audit log by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<AuditLogResponse> getAuditLogById(@PathVariable Long id) {
        return auditLogRepository.findById(id)
            .map(auditLog -> ResponseEntity.ok(createAuditLogResponse(auditLog)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get audit statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<Map<String, Object>> getAuditStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalLogs = auditLogRepository.count();
        long successLogs = auditLogRepository.countByStatus(AuditStatus.SUCCESS);
        long failureLogs = auditLogRepository.countByStatus(AuditStatus.FAILURE);
        long warningLogs = auditLogRepository.countByStatus(AuditStatus.WARNING);
        
        // Count by action
        long loginAttempts = auditLogRepository.countByAction("LOGIN_SUCCESS") + 
                           auditLogRepository.countByAction("LOGIN_FAILED");
        long documentUploads = auditLogRepository.countByAction("UPLOAD_DOCUMENT");
        long documentDownloads = auditLogRepository.countByAction("DOWNLOAD_DOCUMENT");
        long documentDeletions = auditLogRepository.countByAction("DELETE_DOCUMENT");
        
        stats.put("totalLogs", totalLogs);
        stats.put("successLogs", successLogs);
        stats.put("failureLogs", failureLogs);
        stats.put("warningLogs", warningLogs);
        stats.put("loginAttempts", loginAttempts);
        stats.put("documentUploads", documentUploads);
        stats.put("documentDownloads", documentDownloads);
        stats.put("documentDeletions", documentDeletions);
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Export audit logs (Admin only)
     */
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> exportAuditLogs(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String format) {
        
        // This would typically generate a file and return download link
        // For now, return a placeholder response
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Audit log export functionality will be implemented");
        response.put("format", format != null ? format : "CSV");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Create audit log response DTO
     */
    private AuditLogResponse createAuditLogResponse(AuditLog auditLog) {
        AuditLogResponse response = new AuditLogResponse();
        response.setId(auditLog.getId());
        response.setAction(auditLog.getAction());
        response.setResourceType(auditLog.getResourceType());
        response.setResourceId(auditLog.getResourceId());
        response.setDescription(auditLog.getDescription());
        response.setIpAddress(auditLog.getIpAddress());
        response.setUserAgent(auditLog.getUserAgent());
        response.setStatus(auditLog.getStatus());
        response.setErrorMessage(auditLog.getErrorMessage());
        response.setCreatedAt(auditLog.getCreatedAt());
        
        // Include user information if available
        if (auditLog.getUser() != null) {
            response.setUserId(auditLog.getUser().getId());
            response.setUsername(auditLog.getUser().getUsername());
            response.setUserEmail(auditLog.getUser().getEmail());
        }
        
        return response;
    }
    
    /**
     * Audit log response DTO
     */
    public static class AuditLogResponse {
        private Long id;
        private String action;
        private String resourceType;
        private Long resourceId;
        private String description;
        private String ipAddress;
        private String userAgent;
        private AuditStatus status;
        private String errorMessage;
        private LocalDateTime createdAt;
        
        // User information
        private Long userId;
        private String username;
        private String userEmail;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getResourceType() { return resourceType; }
        public void setResourceType(String resourceType) { this.resourceType = resourceType; }
        public Long getResourceId() { return resourceId; }
        public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        public AuditStatus getStatus() { return status; }
        public void setStatus(AuditStatus status) { this.status = status; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getUserEmail() { return userEmail; }
        public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    }
}
