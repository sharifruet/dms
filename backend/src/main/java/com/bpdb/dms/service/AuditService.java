package com.bpdb.dms.service;

import com.bpdb.dms.entity.AuditLog;
import com.bpdb.dms.entity.AuditStatus;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * Service for handling audit logging
 */
@Service
public class AuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    /**
     * Log user action
     */
    public void logUserAction(User user, String action, String resourceType, Long resourceId, 
                             String description, HttpServletRequest request) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUser(user);
            auditLog.setAction(action);
            auditLog.setResourceType(resourceType);
            auditLog.setResourceId(resourceId);
            auditLog.setDescription(description);
            auditLog.setStatus(AuditStatus.SUCCESS);
            
            if (request != null) {
                auditLog.setIpAddress(getClientIpAddress(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
            }
            
            auditLogRepository.save(auditLog);
            logger.info("Audit log created: {} - {} by user: {}", action, description, 
                       user != null ? user.getUsername() : "SYSTEM");
            
        } catch (Exception e) {
            logger.error("Failed to create audit log: {}", e.getMessage());
        }
    }
    
    /**
     * Log failed action
     */
    public void logFailedAction(User user, String action, String resourceType, Long resourceId, 
                               String description, String errorMessage, HttpServletRequest request) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUser(user);
            auditLog.setAction(action);
            auditLog.setResourceType(resourceType);
            auditLog.setResourceId(resourceId);
            auditLog.setDescription(description);
            auditLog.setStatus(AuditStatus.FAILURE);
            auditLog.setErrorMessage(errorMessage);
            
            if (request != null) {
                auditLog.setIpAddress(getClientIpAddress(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
            }
            
            auditLogRepository.save(auditLog);
            logger.warn("Failed action logged: {} - {} by user: {} - Error: {}", 
                       action, description, user != null ? user.getUsername() : "SYSTEM", errorMessage);
            
        } catch (Exception e) {
            logger.error("Failed to create audit log for failed action: {}", e.getMessage());
        }
    }
    
    /**
     * Log system event
     */
    public void logSystemEvent(String action, String resourceType, Long resourceId, String description) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setAction(action);
            auditLog.setResourceType(resourceType);
            auditLog.setResourceId(resourceId);
            auditLog.setDescription(description);
            auditLog.setStatus(AuditStatus.SUCCESS);
            
            auditLogRepository.save(auditLog);
            logger.info("System event logged: {} - {}", action, description);
            
        } catch (Exception e) {
            logger.error("Failed to create system audit log: {}", e.getMessage());
        }
    }
    
    /**
     * Simple activity logging method
     */
    public void logActivity(String username, String action, String description, Object details) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setAction(action);
            auditLog.setDescription(description);
            auditLog.setStatus(AuditStatus.SUCCESS);
            // createdAt will be set automatically by @CreatedDate
            
            // Find user by username if needed
            // For now, just log the activity without user reference
            
            auditLogRepository.save(auditLog);
            logger.info("Activity logged: {} - {} by user: {}", action, description, username);
            
        } catch (Exception e) {
            logger.error("Failed to log activity: {}", e.getMessage());
        }
    }
    
    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Log login attempt
     */
    public void logLoginAttempt(String username, boolean success, String errorMessage, HttpServletRequest request) {
        String action = success ? "LOGIN_SUCCESS" : "LOGIN_FAILED";
        String description = success ? "User logged in successfully" : "Login attempt failed";
        
        if (success) {
            logUserAction(null, action, "USER", null, description, request);
        } else {
            logFailedAction(null, action, "USER", null, description, errorMessage, request);
        }
    }
    
    /**
     * Log logout
     */
    public void logLogout(User user, HttpServletRequest request) {
        logUserAction(user, "LOGOUT", "USER", user.getId(), "User logged out", request);
    }
    
    /**
     * Log document upload
     */
    public void logDocumentUpload(User user, Long documentId, String fileName, HttpServletRequest request) {
        logUserAction(user, "UPLOAD_DOCUMENT", "DOCUMENT", documentId, 
                    "Document uploaded: " + fileName, request);
    }
    
    /**
     * Log document download
     */
    public void logDocumentDownload(User user, Long documentId, String fileName, HttpServletRequest request) {
        logUserAction(user, "DOWNLOAD_DOCUMENT", "DOCUMENT", documentId, 
                    "Document downloaded: " + fileName, request);
    }
    
    /**
     * Log document deletion
     */
    public void logDocumentDeletion(User user, Long documentId, String fileName, HttpServletRequest request) {
        logUserAction(user, "DELETE_DOCUMENT", "DOCUMENT", documentId, 
                    "Document deleted: " + fileName, request);
    }
    
    /**
     * Log user management actions
     */
    public void logUserManagement(User admin, String action, Long targetUserId, String targetUsername, HttpServletRequest request) {
        logUserAction(admin, action, "USER", targetUserId, 
                    "User management action on: " + targetUsername, request);
    }
}
