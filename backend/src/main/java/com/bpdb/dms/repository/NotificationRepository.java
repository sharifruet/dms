package com.bpdb.dms.repository;

import com.bpdb.dms.entity.Notification;
import com.bpdb.dms.entity.NotificationStatus;
import com.bpdb.dms.entity.NotificationType;
import com.bpdb.dms.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Notification entity
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    /**
     * Find notifications by user
     */
    Page<Notification> findByUser(User user, Pageable pageable);
    
    /**
     * Find notifications by user and status
     */
    Page<Notification> findByUserAndStatus(User user, NotificationStatus status, Pageable pageable);
    
    /**
     * Find notifications by user and type
     */
    Page<Notification> findByUserAndType(User user, NotificationType type, Pageable pageable);
    
    /**
     * Find unread notifications by user
     */
    List<Notification> findByUserAndReadAtIsNullOrderByCreatedAtDesc(User user);
    
    /**
     * Count unread notifications by user
     */
    long countByUserAndReadAtIsNull(User user);
    
    /**
     * Find notifications by status
     */
    List<Notification> findByStatus(NotificationStatus status);
    
    /**
     * Find notifications scheduled for sending
     */
    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' AND n.scheduledAt <= :now")
    List<Notification> findScheduledNotifications(@Param("now") LocalDateTime now);
    
    /**
     * Find expired notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now AND n.status != 'EXPIRED'")
    List<Notification> findExpiredNotifications(@Param("now") LocalDateTime now);
    
    /**
     * Find notifications by related document
     */
    List<Notification> findByRelatedDocumentId(Long documentId);
    
    /**
     * Find notifications by related entity type
     */
    List<Notification> findByRelatedEntityType(String entityType);
    
    /**
     * Find notifications created between dates
     */
    List<Notification> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find notifications by channel
     */
    List<Notification> findByChannel(String channel);
    
    /**
     * Delete old notifications
     */
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate AND n.status IN ('READ', 'EXPIRED')")
    void deleteOldNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);
}
