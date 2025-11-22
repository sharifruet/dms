package com.bpdb.dms.service;

import com.bpdb.dms.entity.*;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.ExpiryTrackingRepository;
import com.bpdb.dms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing document expiry tracking and alerts
 */
@Service
@Transactional
public class ExpiryTrackingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExpiryTrackingService.class);
    
    @Autowired
    private ExpiryTrackingRepository expiryTrackingRepository;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private AuditService auditService;
    
    @Autowired
    private DocumentMetadataService documentMetadataService;
    
    /**
     * Create expiry tracking for a document
     */
    public ExpiryTracking createExpiryTracking(Document document, ExpiryType expiryType, 
                                             LocalDateTime expiryDate, User assignedTo) {
        try {
            ExpiryTracking tracking = new ExpiryTracking();
            tracking.setExpiryType(expiryType);
            tracking.setExpiryDate(expiryDate);
            tracking.setAssignedTo(assignedTo);
            
            if (document != null) {
                tracking.setDocument(document);
                tracking.setDepartment(document.getDepartment());
                
                // Extract additional information from document metadata if available
                if (document.getDescription() != null) {
                    // Try to extract vendor name and contract value from description
                    // This is a simplified implementation - in production, use more sophisticated parsing
                    tracking.setVendorName(extractVendorName(document.getDescription()));
                    tracking.setContractValue(extractContractValue(document.getDescription()));
                }
            }
            
            ExpiryTracking savedTracking = expiryTrackingRepository.save(tracking);
            
            // Log activity
            auditService.logActivity(assignedTo.getUsername(), "EXPIRY_TRACKING_CREATED", 
                "Expiry tracking created for document: " + (document != null ? document.getOriginalName() : "Unknown"), null);
            
            logger.info("Expiry tracking created for document {}: {}", 
                document != null ? document.getId() : "Unknown", expiryType.getDisplayName());
            
            return savedTracking;
            
        } catch (Exception e) {
            logger.error("Failed to create expiry tracking for document {}: {}", 
                document != null ? document.getId() : "Unknown", e.getMessage());
            throw new RuntimeException("Failed to create expiry tracking", e);
        }
    }
    
    /**
     * Update expiry tracking
     */
    public ExpiryTracking updateExpiryTracking(Long trackingId, ExpiryTracking updatedTracking) {
        try {
            Optional<ExpiryTracking> existingOpt = expiryTrackingRepository.findById(trackingId);
            if (existingOpt.isPresent()) {
                ExpiryTracking existing = existingOpt.get();
                
                existing.setExpiryDate(updatedTracking.getExpiryDate());
                existing.setAssignedTo(updatedTracking.getAssignedTo());
                existing.setNotes(updatedTracking.getNotes());
                existing.setVendorName(updatedTracking.getVendorName());
                existing.setContractValue(updatedTracking.getContractValue());
                existing.setCurrency(updatedTracking.getCurrency());
                
                return expiryTrackingRepository.save(existing);
            }
            return null;
        } catch (Exception e) {
            logger.error("Failed to update expiry tracking {}: {}", trackingId, e.getMessage());
            throw new RuntimeException("Failed to update expiry tracking", e);
        }
    }
    
    /**
     * Renew expiry tracking
     */
    public ExpiryTracking renewExpiryTracking(Long trackingId, LocalDateTime newExpiryDate, 
                                             Long renewalDocumentId, String notes) {
        try {
            Optional<ExpiryTracking> existingOpt = expiryTrackingRepository.findById(trackingId);
            if (existingOpt.isPresent()) {
                ExpiryTracking existing = existingOpt.get();
                
                existing.setRenewalDate(LocalDateTime.now());
                existing.setRenewalDocumentId(renewalDocumentId);
                existing.setExpiryDate(newExpiryDate);
                existing.setStatus(ExpiryStatus.RENEWED);
                existing.setNotes(notes);
                
                // Reset alert flags
                existing.setAlert30Days(false);
                existing.setAlert15Days(false);
                existing.setAlert7Days(false);
                existing.setAlertExpired(false);
                
                ExpiryTracking renewed = expiryTrackingRepository.save(existing);
                
                // Send renewal notification
                sendRenewalNotification(existing);
                
                logger.info("Expiry tracking {} renewed until {}", trackingId, newExpiryDate);
                
                return renewed;
            }
            return null;
        } catch (Exception e) {
            logger.error("Failed to renew expiry tracking {}: {}", trackingId, e.getMessage());
            throw new RuntimeException("Failed to renew expiry tracking", e);
        }
    }
    
    /**
     * Get expiry tracking by document
     */
    public List<ExpiryTracking> getExpiryTrackingByDocument(Long documentId) {
        return expiryTrackingRepository.findByDocumentId(documentId);
    }
    
    /**
     * Get active expiry tracking
     */
    public Page<ExpiryTracking> getActiveExpiryTracking(Pageable pageable) {
        return expiryTrackingRepository.findByStatus(ExpiryStatus.ACTIVE, pageable);
    }
    
    /**
     * Get expiring documents within specified days
     */
    public List<ExpiryTracking> getExpiringDocuments(int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusDays(days);
        return expiryTrackingRepository.findExpiringBetween(now, futureDate);
    }
    
    /**
     * Get expired documents
     */
    public List<ExpiryTracking> getExpiredDocuments() {
        return expiryTrackingRepository.findExpired(LocalDateTime.now());
    }
    
    /**
     * Process expiry alerts - runs every hour
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void processExpiryAlerts() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Process 30-day alerts
            process30DayAlerts(now);
            
            // Process 15-day alerts
            process15DayAlerts(now);
            
            // Process 7-day alerts
            process7DayAlerts(now);
            
            // Process expired alerts
            processExpiredAlerts(now);
            
            logger.info("Expiry alerts processing completed");
            
        } catch (Exception e) {
            logger.error("Failed to process expiry alerts: {}", e.getMessage());
        }
    }
    
    /**
     * Process 30-day expiry alerts
     */
    private void process30DayAlerts(LocalDateTime now) {
        try {
            LocalDateTime thirtyDaysFromNow = now.plusDays(30);
            List<ExpiryTracking> expiringIn30Days = 
                expiryTrackingRepository.findExpiringIn30Days(now, thirtyDaysFromNow);
            
            for (ExpiryTracking tracking : expiringIn30Days) {
                sendExpiryAlert(tracking, 30);
                tracking.setAlert30Days(true);
                expiryTrackingRepository.save(tracking);
            }
            
            if (!expiringIn30Days.isEmpty()) {
                logger.info("Processed {} 30-day expiry alerts", expiringIn30Days.size());
            }
            
        } catch (Exception e) {
            logger.error("Failed to process 30-day alerts: {}", e.getMessage());
        }
    }
    
    /**
     * Process 15-day expiry alerts
     */
    private void process15DayAlerts(LocalDateTime now) {
        try {
            LocalDateTime fifteenDaysFromNow = now.plusDays(15);
            List<ExpiryTracking> expiringIn15Days = 
                expiryTrackingRepository.findExpiringIn15Days(now, fifteenDaysFromNow);
            
            for (ExpiryTracking tracking : expiringIn15Days) {
                sendExpiryAlert(tracking, 15);
                tracking.setAlert15Days(true);
                expiryTrackingRepository.save(tracking);
            }
            
            if (!expiringIn15Days.isEmpty()) {
                logger.info("Processed {} 15-day expiry alerts", expiringIn15Days.size());
            }
            
        } catch (Exception e) {
            logger.error("Failed to process 15-day alerts: {}", e.getMessage());
        }
    }
    
    /**
     * Process 7-day expiry alerts
     */
    private void process7DayAlerts(LocalDateTime now) {
        try {
            LocalDateTime sevenDaysFromNow = now.plusDays(7);
            List<ExpiryTracking> expiringIn7Days = 
                expiryTrackingRepository.findExpiringIn7Days(now, sevenDaysFromNow);
            
            for (ExpiryTracking tracking : expiringIn7Days) {
                sendExpiryAlert(tracking, 7);
                tracking.setAlert7Days(true);
                expiryTrackingRepository.save(tracking);
            }
            
            if (!expiringIn7Days.isEmpty()) {
                logger.info("Processed {} 7-day expiry alerts", expiringIn7Days.size());
            }
            
        } catch (Exception e) {
            logger.error("Failed to process 7-day alerts: {}", e.getMessage());
        }
    }
    
    /**
     * Process expired alerts
     */
    private void processExpiredAlerts(LocalDateTime now) {
        try {
            List<ExpiryTracking> recentlyExpired = 
                expiryTrackingRepository.findRecentlyExpired(now);
            
            for (ExpiryTracking tracking : recentlyExpired) {
                sendExpiredAlert(tracking);
                tracking.setAlertExpired(true);
                tracking.setStatus(ExpiryStatus.EXPIRED);
                expiryTrackingRepository.save(tracking);
            }
            
            if (!recentlyExpired.isEmpty()) {
                logger.info("Processed {} expired alerts", recentlyExpired.size());
            }
            
        } catch (Exception e) {
            logger.error("Failed to process expired alerts: {}", e.getMessage());
        }
    }
    
    /**
     * Send expiry alert notification
     */
    private void sendExpiryAlert(ExpiryTracking tracking, int daysRemaining) {
        try {
            User user = tracking.getAssignedTo();
            if (user == null) {
                logger.warn("No assigned user for expiry tracking {}", tracking.getId());
                return;
            }
            
            String title = String.format("%s Expiry Alert - %d days remaining", 
                tracking.getExpiryType().getDisplayName(), daysRemaining);
            
            String message = String.format("Document '%s' (%s) will expire in %d days on %s. Please take necessary action.",
                tracking.getDocument().getOriginalName(),
                tracking.getExpiryType().getDisplayName(),
                daysRemaining,
                tracking.getExpiryDate().toLocalDate().toString());
            
            NotificationType notificationType = getNotificationTypeForExpiryType(tracking.getExpiryType());
            NotificationPriority priority = getPriorityForDaysRemaining(daysRemaining);
            
            notificationService.createNotification(user, title, message, notificationType, priority,
                tracking.getDocument().getId(), "EXPIRY_TRACKING", null);
            
        } catch (Exception e) {
            logger.error("Failed to send expiry alert for tracking {}: {}", tracking.getId(), e.getMessage());
        }
    }
    
    /**
     * Send expired alert notification
     */
    private void sendExpiredAlert(ExpiryTracking tracking) {
        try {
            User user = tracking.getAssignedTo();
            if (user == null) {
                logger.warn("No assigned user for expiry tracking {}", tracking.getId());
                return;
            }
            
            String title = String.format("%s EXPIRED", tracking.getExpiryType().getDisplayName());
            
            String message = String.format("Document '%s' (%s) has expired on %s. Immediate action required.",
                tracking.getDocument().getOriginalName(),
                tracking.getExpiryType().getDisplayName(),
                tracking.getExpiryDate().toLocalDate().toString());
            
            NotificationType notificationType = getNotificationTypeForExpiryType(tracking.getExpiryType());
            
            notificationService.createNotification(user, title, message, notificationType, 
                NotificationPriority.CRITICAL, tracking.getDocument().getId(), "EXPIRY_TRACKING", null);
            
        } catch (Exception e) {
            logger.error("Failed to send expired alert for tracking {}: {}", tracking.getId(), e.getMessage());
        }
    }
    
    /**
     * Send renewal notification
     */
    private void sendRenewalNotification(ExpiryTracking tracking) {
        try {
            User user = tracking.getAssignedTo();
            if (user == null) {
                return;
            }
            
            String title = String.format("%s Renewed", tracking.getExpiryType().getDisplayName());
            
            String message = String.format("Document '%s' (%s) has been renewed until %s.",
                tracking.getDocument().getOriginalName(),
                tracking.getExpiryType().getDisplayName(),
                tracking.getExpiryDate().toLocalDate().toString());
            
            NotificationType notificationType = getNotificationTypeForExpiryType(tracking.getExpiryType());
            
            notificationService.createNotification(user, title, message, notificationType, 
                NotificationPriority.MEDIUM, tracking.getDocument().getId(), "EXPIRY_TRACKING", null);
            
        } catch (Exception e) {
            logger.error("Failed to send renewal notification for tracking {}: {}", tracking.getId(), e.getMessage());
        }
    }
    
    /**
     * Get notification type for expiry type
     */
    private NotificationType getNotificationTypeForExpiryType(ExpiryType expiryType) {
        switch (expiryType) {
            case CONTRACT:
                return NotificationType.CONTRACT_EXPIRY;
            case BANK_GUARANTEE:
                return NotificationType.BG_EXPIRY;
            case LETTER_OF_CREDIT:
                return NotificationType.LC_EXPIRY;
            case PERFORMANCE_SECURITY:
                return NotificationType.PS_EXPIRY;
            default:
                return NotificationType.DOCUMENT_EXPIRY;
        }
    }
    
    /**
     * Get priority based on days remaining
     */
    private NotificationPriority getPriorityForDaysRemaining(int daysRemaining) {
        if (daysRemaining <= 7) {
            return NotificationPriority.CRITICAL;
        } else if (daysRemaining <= 15) {
            return NotificationPriority.HIGH;
        } else if (daysRemaining <= 30) {
            return NotificationPriority.MEDIUM;
        } else {
            return NotificationPriority.LOW;
        }
    }
    
    /**
     * Extract vendor name from document description (simplified)
     */
    private String extractVendorName(String description) {
        // This is a simplified implementation
        // In production, use more sophisticated NLP or regex patterns
        if (description != null && description.toLowerCase().contains("vendor")) {
            // Extract vendor name logic here
            return "Unknown Vendor";
        }
        return null;
    }
    
    /**
     * Extract contract value from document description (simplified)
     */
    private Double extractContractValue(String description) {
        // This is a simplified implementation
        // In production, use more sophisticated parsing
        if (description != null && description.toLowerCase().contains("value")) {
            // Extract contract value logic here
            return null;
        }
        return null;
    }
    
    /**
     * Get expiry statistics
     */
    public Map<String, Object> getExpiryStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Count by status
            stats.put("active", expiryTrackingRepository.countByExpiryTypeAndStatus(ExpiryType.CONTRACT, ExpiryStatus.ACTIVE));
            stats.put("expired", expiryTrackingRepository.countByExpiryTypeAndStatus(ExpiryType.CONTRACT, ExpiryStatus.EXPIRED));
            stats.put("renewed", expiryTrackingRepository.countByExpiryTypeAndStatus(ExpiryType.CONTRACT, ExpiryStatus.RENEWED));
            
            // Count expiring in next 30 days
            List<ExpiryTracking> expiringIn30Days = getExpiringDocuments(30);
            stats.put("expiringIn30Days", expiringIn30Days.size());
            
            // Count expired
            List<ExpiryTracking> expired = getExpiredDocuments();
            stats.put("expiredCount", expired.size());
            
        } catch (Exception e) {
            logger.error("Failed to get expiry statistics: {}", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Get Performance Security documents with expiry dates from metadata
     */
    public List<Map<String, Object>> getPerformanceSecurityDocumentsWithExpiry() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            // Get all PERFORMANCE_SECURITY_PS documents
            Page<Document> psDocuments = documentRepository.findByDocumentType("PERFORMANCE_SECURITY_PS", 
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE));
            
            LocalDateTime now = LocalDateTime.now();
            
            for (Document doc : psDocuments.getContent()) {
                try {
                    // Get document metadata
                    Map<String, String> metadata = documentMetadataService.getMetadataMap(doc);
                    
                    // Check for expiryDate in metadata
                    String expiryDateStr = metadata.get("expiryDate");
                    if (expiryDateStr != null && !expiryDateStr.trim().isEmpty()) {
                        try {
                            // Parse expiry date - try LocalDate first, then LocalDateTime
                            LocalDateTime expiryDate;
                            try {
                                // Try parsing as LocalDate first (most common format)
                                java.time.LocalDate localDate = java.time.LocalDate.parse(expiryDateStr);
                                expiryDate = localDate.atStartOfDay();
                            } catch (Exception e1) {
                                // Try parsing as LocalDateTime
                                expiryDate = LocalDateTime.parse(expiryDateStr);
                            }
                            
                            // Create a map similar to ExpiryTracking for frontend
                            Map<String, Object> docWithExpiry = new HashMap<>();
                            docWithExpiry.put("id", doc.getId());
                            docWithExpiry.put("documentId", doc.getId());
                            docWithExpiry.put("document", doc);
                            docWithExpiry.put("expiryType", "PERFORMANCE_SECURITY");
                            docWithExpiry.put("expiryDate", expiryDate);
                            docWithExpiry.put("status", expiryDate.isBefore(now) ? "EXPIRED" : "ACTIVE");
                            docWithExpiry.put("department", doc.getDepartment());
                            docWithExpiry.put("vendorName", metadata.get("vendorName"));
                            
                            // Try to parse contract value
                            String contractValueStr = metadata.get("contractValue");
                            if (contractValueStr != null && !contractValueStr.trim().isEmpty()) {
                                try {
                                    docWithExpiry.put("contractValue", Double.parseDouble(contractValueStr));
                                } catch (NumberFormatException e) {
                                    // Ignore parsing errors
                                }
                            }
                            
                            docWithExpiry.put("currency", metadata.get("currency"));
                            docWithExpiry.put("createdAt", doc.getCreatedAt());
                            docWithExpiry.put("updatedAt", doc.getUpdatedAt());
                            docWithExpiry.put("isFromMetadata", true); // Flag to indicate this is from metadata, not expiry_tracking table
                            
                            result.add(docWithExpiry);
                        } catch (Exception e) {
                            logger.warn("Failed to parse expiry date for document {}: {}", doc.getId(), expiryDateStr, e);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Failed to get metadata for document {}: {}", doc.getId(), e.getMessage());
                }
            }
            
            // Sort by expiry date (ascending - earliest expiry first)
            result.sort((a, b) -> {
                LocalDateTime dateA = (LocalDateTime) a.get("expiryDate");
                LocalDateTime dateB = (LocalDateTime) b.get("expiryDate");
                return dateA.compareTo(dateB);
            });
            
        } catch (Exception e) {
            logger.error("Failed to get Performance Security documents with expiry: {}", e.getMessage(), e);
        }
        
        return result;
    }
}
