package com.bpdb.dms.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity for managing user notification preferences
 */
@Entity
@Table(name = "notification_preferences")
@EntityListeners(AuditingEntityListener.class)
public class NotificationPreference {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;
    
    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled = true;
    
    @Column(name = "sms_enabled", nullable = false)
    private Boolean smsEnabled = false;
    
    @Column(name = "in_app_enabled", nullable = false)
    private Boolean inAppEnabled = true;
    
    @Column(name = "push_enabled", nullable = false)
    private Boolean pushEnabled = false;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "minimum_priority")
    private NotificationPriority minimumPriority = NotificationPriority.MEDIUM;
    
    @Column(name = "quiet_hours_start")
    private String quietHoursStart; // Format: "HH:mm"
    
    @Column(name = "quiet_hours_end")
    private String quietHoursEnd; // Format: "HH:mm"
    
    @Column(name = "timezone")
    private String timezone = "UTC";
    
    @Column(name = "language")
    private String language = "en";
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public NotificationPreference() {}
    
    public NotificationPreference(User user, NotificationType notificationType) {
        this.user = user;
        this.notificationType = notificationType;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public NotificationType getNotificationType() { return notificationType; }
    public void setNotificationType(NotificationType notificationType) { this.notificationType = notificationType; }
    
    public Boolean getEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(Boolean emailEnabled) { this.emailEnabled = emailEnabled; }
    
    public Boolean getSmsEnabled() { return smsEnabled; }
    public void setSmsEnabled(Boolean smsEnabled) { this.smsEnabled = smsEnabled; }
    
    public Boolean getInAppEnabled() { return inAppEnabled; }
    public void setInAppEnabled(Boolean inAppEnabled) { this.inAppEnabled = inAppEnabled; }
    
    public Boolean getPushEnabled() { return pushEnabled; }
    public void setPushEnabled(Boolean pushEnabled) { this.pushEnabled = pushEnabled; }
    
    public NotificationPriority getMinimumPriority() { return minimumPriority; }
    public void setMinimumPriority(NotificationPriority minimumPriority) { this.minimumPriority = minimumPriority; }
    
    public String getQuietHoursStart() { return quietHoursStart; }
    public void setQuietHoursStart(String quietHoursStart) { this.quietHoursStart = quietHoursStart; }
    
    public String getQuietHoursEnd() { return quietHoursEnd; }
    public void setQuietHoursEnd(String quietHoursEnd) { this.quietHoursEnd = quietHoursEnd; }
    
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
