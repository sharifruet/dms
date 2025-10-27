package com.bpdb.dms.repository;

import com.bpdb.dms.entity.Webhook;
import com.bpdb.dms.entity.WebhookStatus;
import com.bpdb.dms.entity.WebhookEventType;
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
 * Repository interface for Webhook entity
 */
@Repository
public interface WebhookRepository extends JpaRepository<Webhook, Long> {
    
    /**
     * Find webhooks by status
     */
    Page<Webhook> findByStatus(WebhookStatus status, Pageable pageable);
    
    /**
     * Find webhooks by event type
     */
    List<Webhook> findByEventType(WebhookEventType eventType);
    
    /**
     * Find enabled webhooks by event type
     */
    List<Webhook> findByEventTypeAndIsEnabledTrue(WebhookEventType eventType);
    
    /**
     * Find webhooks by creator
     */
    Page<Webhook> findByCreatedBy(User createdBy, Pageable pageable);
    
    /**
     * Find enabled webhooks
     */
    List<Webhook> findByIsEnabledTrue();
    
    /**
     * Find webhooks by URL
     */
    List<Webhook> findByUrl(String url);
    
    /**
     * Find webhooks that haven't been triggered recently
     */
    @Query("SELECT w FROM Webhook w WHERE w.lastTriggeredAt < :threshold OR w.lastTriggeredAt IS NULL")
    List<Webhook> findInactiveWebhooks(@Param("threshold") LocalDateTime threshold);
    
    /**
     * Find webhooks with high failure rate
     */
    @Query("SELECT w FROM Webhook w WHERE w.failureCount > :threshold AND w.isEnabled = true")
    List<Webhook> findWebhooksWithHighFailureRate(@Param("threshold") Long threshold);
    
    /**
     * Count webhooks by status
     */
    long countByStatus(WebhookStatus status);
    
    /**
     * Count enabled webhooks
     */
    long countByIsEnabledTrue();
    
    /**
     * Find webhooks by multiple criteria
     */
    @Query("SELECT w FROM Webhook w WHERE " +
           "(:status IS NULL OR w.status = :status) AND " +
           "(:eventType IS NULL OR w.eventType = :eventType) AND " +
           "(:isEnabled IS NULL OR w.isEnabled = :isEnabled) AND " +
           "(:createdBy IS NULL OR w.createdBy = :createdBy)")
    Page<Webhook> findByMultipleCriteria(@Param("status") WebhookStatus status,
                                        @Param("eventType") WebhookEventType eventType,
                                        @Param("isEnabled") Boolean isEnabled,
                                        @Param("createdBy") User createdBy,
                                        Pageable pageable);
}
