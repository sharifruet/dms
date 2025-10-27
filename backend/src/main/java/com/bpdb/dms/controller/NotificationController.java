package com.bpdb.dms.controller;

import com.bpdb.dms.entity.*;
import com.bpdb.dms.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for notification management
 */
@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * Get user notifications
     */
    @GetMapping
    public ResponseEntity<Page<Notification>> getUserNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        try {
            User user = (User) authentication.getPrincipal();
            Pageable pageable = PageRequest.of(page, size);
            Page<Notification> notifications = notificationService.getUserNotifications(user, pageable);
            
            return ResponseEntity.ok(notifications);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get unread notifications count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            long count = notificationService.getUnreadCount(user);
            
            return ResponseEntity.ok(Map.of("unreadCount", count));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get unread notifications
     */
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            List<Notification> notifications = notificationService.getUnreadNotifications(user);
            
            return ResponseEntity.ok(notifications);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Mark notification as read
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable Long notificationId,
            Authentication authentication) {
        
        try {
            User user = (User) authentication.getPrincipal();
            notificationService.markAsRead(notificationId, user.getUsername());
            
            return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to mark notification as read"));
        }
    }
    
    /**
     * Create notification
     */
    @PostMapping
    public ResponseEntity<Notification> createNotification(
            @RequestBody CreateNotificationRequest request,
            Authentication authentication) {
        
        try {
            User user = (User) authentication.getPrincipal();
            
            Notification notification = notificationService.createNotification(
                user,
                request.getTitle(),
                request.getMessage(),
                request.getType(),
                request.getPriority()
            );
            
            return ResponseEntity.ok(notification);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get notification preferences
     */
    @GetMapping("/preferences")
    public ResponseEntity<List<NotificationPreference>> getNotificationPreferences(Authentication authentication) {
        try {
            // TODO: Implement get preferences method
            return ResponseEntity.ok(List.of());
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Update notification preferences
     */
    @PutMapping("/preferences/{preferenceId}")
    public ResponseEntity<NotificationPreference> updateNotificationPreferences(
            @PathVariable Long preferenceId,
            @RequestBody NotificationPreference preference,
            Authentication authentication) {
        
        try {
            NotificationPreference updated = notificationService.updatePreference(preferenceId, preference);
            
            if (updated != null) {
                return ResponseEntity.ok(updated);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Create default notification preferences
     */
    @PostMapping("/preferences/default")
    public ResponseEntity<Map<String, String>> createDefaultPreferences(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            notificationService.createDefaultPreferences(user);
            
            return ResponseEntity.ok(Map.of("message", "Default preferences created"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to create default preferences"));
        }
    }
    
    /**
     * Create notification request DTO
     */
    public static class CreateNotificationRequest {
        private String title;
        private String message;
        private NotificationType type;
        private NotificationPriority priority;
        
        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public NotificationType getType() { return type; }
        public void setType(NotificationType type) { this.type = type; }
        public NotificationPriority getPriority() { return priority; }
        public void setPriority(NotificationPriority priority) { this.priority = priority; }
    }
}
