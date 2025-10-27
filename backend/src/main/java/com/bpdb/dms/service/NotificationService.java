package com.bpdb.dms.service;

import com.bpdb.dms.entity.*;
import com.bpdb.dms.repository.NotificationPreferenceRepository;
import com.bpdb.dms.repository.NotificationRepository;
import com.bpdb.dms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing notifications
 */
@Service
@Transactional
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;
    
    @Autowired
    private EmailNotificationService emailNotificationService;
    
    @Autowired
    private SmsNotificationService smsNotificationService;
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Create and send a notification
     */
    public Notification createNotification(User user, String title, String message, 
                                         NotificationType type, NotificationPriority priority) {
        return createNotification(user, title, message, type, priority, null, null, null);
    }
    
    /**
     * Create and send a notification with additional parameters
     */
    public Notification createNotification(User user, String title, String message, 
                                         NotificationType type, NotificationPriority priority,
                                         Long relatedDocumentId, String relatedEntityType, 
                                         Map<String, Object> metadata) {
        try {
            Notification notification = new Notification(user, title, message, type, priority);
            notification.setRelatedDocumentId(relatedDocumentId);
            notification.setRelatedEntityType(relatedEntityType);
            
            if (metadata != null) {
                notification.setMetadata(convertMetadataToString(metadata));
            }
            
            // Set expiry date (default 30 days)
            notification.setExpiresAt(LocalDateTime.now().plusDays(30));
            
            Notification savedNotification = notificationRepository.save(notification);
            
            // Send notification asynchronously
            sendNotificationAsync(savedNotification);
            
            // Log activity
            auditService.logActivity(user.getUsername(), "NOTIFICATION_CREATED", 
                "Notification created: " + type.getDisplayName(), metadata);
            
            logger.info("Notification created for user {}: {}", user.getUsername(), title);
            
            return savedNotification;
            
        } catch (Exception e) {
            logger.error("Failed to create notification for user {}: {}", user.getUsername(), e.getMessage());
            throw new RuntimeException("Failed to create notification", e);
        }
    }
    
    /**
     * Send notification asynchronously
     */
    @Async
    public CompletableFuture<Void> sendNotificationAsync(Notification notification) {
        try {
            User user = notification.getUser();
            NotificationType type = notification.getType();
            
            // Check user preferences
            Optional<NotificationPreference> preference = 
                notificationPreferenceRepository.findByUserAndNotificationType(user, type);
            
            if (preference.isPresent()) {
                NotificationPreference pref = preference.get();
                
                // Send email if enabled
                if (pref.getEmailEnabled() && isPriorityHighEnough(notification.getPriority(), pref.getMinimumPriority())) {
                    sendEmailNotification(notification);
                }
                
                // Send SMS if enabled
                if (pref.getSmsEnabled() && isPriorityHighEnough(notification.getPriority(), pref.getMinimumPriority())) {
                    sendSmsNotification(notification);
                }
                
                // Send in-app notification if enabled
                if (pref.getInAppEnabled()) {
                    sendInAppNotification(notification);
                }
                
                // Send push notification if enabled
                if (pref.getPushEnabled() && isPriorityHighEnough(notification.getPriority(), pref.getMinimumPriority())) {
                    sendPushNotification(notification);
                }
            } else {
                // Default behavior - send in-app notification
                sendInAppNotification(notification);
            }
            
            // Update notification status
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);
            
        } catch (Exception e) {
            logger.error("Failed to send notification {}: {}", notification.getId(), e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
            notificationRepository.save(notification);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Send email notification
     */
    private void sendEmailNotification(Notification notification) {
        try {
            emailNotificationService.sendNotification(notification);
            notification.setChannel("EMAIL");
            logger.info("Email notification sent for notification {}", notification.getId());
        } catch (Exception e) {
            logger.error("Failed to send email notification {}: {}", notification.getId(), e.getMessage());
        }
    }
    
    /**
     * Send SMS notification
     */
    private void sendSmsNotification(Notification notification) {
        try {
            smsNotificationService.sendNotification(notification);
            notification.setChannel("SMS");
            logger.info("SMS notification sent for notification {}", notification.getId());
        } catch (Exception e) {
            logger.error("Failed to send SMS notification {}: {}", notification.getId(), e.getMessage());
        }
    }
    
    /**
     * Send in-app notification
     */
    private void sendInAppNotification(Notification notification) {
        try {
            // In-app notifications are already stored in database
            notification.setChannel("IN_APP");
            logger.info("In-app notification created for notification {}", notification.getId());
        } catch (Exception e) {
            logger.error("Failed to create in-app notification {}: {}", notification.getId(), e.getMessage());
        }
    }
    
    /**
     * Send push notification
     */
    private void sendPushNotification(Notification notification) {
        try {
            // TODO: Implement push notification service
            notification.setChannel("PUSH");
            logger.info("Push notification sent for notification {}", notification.getId());
        } catch (Exception e) {
            logger.error("Failed to send push notification {}: {}", notification.getId(), e.getMessage());
        }
    }
    
    /**
     * Check if notification priority is high enough for user preference
     */
    private boolean isPriorityHighEnough(NotificationPriority notificationPriority, NotificationPriority minimumPriority) {
        return notificationPriority.ordinal() >= minimumPriority.ordinal();
    }
    
    /**
     * Mark notification as read
     */
    public void markAsRead(Long notificationId, String username) {
        try {
            Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
            if (notificationOpt.isPresent()) {
                Notification notification = notificationOpt.get();
                if (notification.getUser().getUsername().equals(username)) {
                    notification.setReadAt(LocalDateTime.now());
                    notification.setStatus(NotificationStatus.READ);
                    notificationRepository.save(notification);
                    
                    logger.info("Notification {} marked as read by user {}", notificationId, username);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to mark notification {} as read: {}", notificationId, e.getMessage());
        }
    }
    
    /**
     * Get user notifications
     */
    public Page<Notification> getUserNotifications(User user, Pageable pageable) {
        return notificationRepository.findByUser(user, pageable);
    }
    
    /**
     * Get unread notifications count for user
     */
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndReadAtIsNull(user);
    }
    
    /**
     * Get unread notifications for user
     */
    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndReadAtIsNullOrderByCreatedAtDesc(user);
    }
    
    /**
     * Process scheduled notifications
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    public void processScheduledNotifications() {
        try {
            List<Notification> scheduledNotifications = 
                notificationRepository.findScheduledNotifications(LocalDateTime.now());
            
            for (Notification notification : scheduledNotifications) {
                sendNotificationAsync(notification);
            }
            
            if (!scheduledNotifications.isEmpty()) {
                logger.info("Processed {} scheduled notifications", scheduledNotifications.size());
            }
        } catch (Exception e) {
            logger.error("Failed to process scheduled notifications: {}", e.getMessage());
        }
    }
    
    /**
     * Process expired notifications
     */
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void processExpiredNotifications() {
        try {
            List<Notification> expiredNotifications = 
                notificationRepository.findExpiredNotifications(LocalDateTime.now());
            
            for (Notification notification : expiredNotifications) {
                notification.setStatus(NotificationStatus.EXPIRED);
                notificationRepository.save(notification);
            }
            
            if (!expiredNotifications.isEmpty()) {
                logger.info("Processed {} expired notifications", expiredNotifications.size());
            }
        } catch (Exception e) {
            logger.error("Failed to process expired notifications: {}", e.getMessage());
        }
    }
    
    /**
     * Clean up old notifications
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    public void cleanupOldNotifications() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90); // Keep notifications for 90 days
            notificationRepository.deleteOldNotifications(cutoffDate);
            logger.info("Cleaned up old notifications older than {}", cutoffDate);
        } catch (Exception e) {
            logger.error("Failed to cleanup old notifications: {}", e.getMessage());
        }
    }
    
    /**
     * Create notification preferences for user
     */
    public void createDefaultPreferences(User user) {
        try {
            for (NotificationType type : NotificationType.values()) {
                Optional<NotificationPreference> existing = 
                    notificationPreferenceRepository.findByUserAndNotificationType(user, type);
                
                if (!existing.isPresent()) {
                    NotificationPreference preference = new NotificationPreference(user, type);
                    notificationPreferenceRepository.save(preference);
                }
            }
            
            logger.info("Created default notification preferences for user {}", user.getUsername());
        } catch (Exception e) {
            logger.error("Failed to create default preferences for user {}: {}", user.getUsername(), e.getMessage());
        }
    }
    
    /**
     * Update notification preferences
     */
    public NotificationPreference updatePreference(Long preferenceId, NotificationPreference updatedPreference) {
        try {
            Optional<NotificationPreference> existingOpt = notificationPreferenceRepository.findById(preferenceId);
            if (existingOpt.isPresent()) {
                NotificationPreference existing = existingOpt.get();
                existing.setEmailEnabled(updatedPreference.getEmailEnabled());
                existing.setSmsEnabled(updatedPreference.getSmsEnabled());
                existing.setInAppEnabled(updatedPreference.getInAppEnabled());
                existing.setPushEnabled(updatedPreference.getPushEnabled());
                existing.setMinimumPriority(updatedPreference.getMinimumPriority());
                existing.setQuietHoursStart(updatedPreference.getQuietHoursStart());
                existing.setQuietHoursEnd(updatedPreference.getQuietHoursEnd());
                existing.setTimezone(updatedPreference.getTimezone());
                existing.setLanguage(updatedPreference.getLanguage());
                
                return notificationPreferenceRepository.save(existing);
            }
            return null;
        } catch (Exception e) {
            logger.error("Failed to update notification preference {}: {}", preferenceId, e.getMessage());
            throw new RuntimeException("Failed to update notification preference", e);
        }
    }
    
    /**
     * Convert metadata map to JSON string
     */
    private String convertMetadataToString(Map<String, Object> metadata) {
        try {
            // Simple JSON conversion - in production, use a proper JSON library
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                if (!first) json.append(",");
                json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
                first = false;
            }
            json.append("}");
            return json.toString();
        } catch (Exception e) {
            logger.error("Failed to convert metadata to string: {}", e.getMessage());
            return "{}";
        }
    }
}
