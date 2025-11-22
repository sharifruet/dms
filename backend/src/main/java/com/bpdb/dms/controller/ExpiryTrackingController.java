package com.bpdb.dms.controller;

import com.bpdb.dms.entity.*;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.ExpiryTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controller for expiry tracking management
 */
@RestController
@RequestMapping("/api/expiry-tracking")
@CrossOrigin(origins = "*")
public class ExpiryTrackingController {
    
    @Autowired
    private ExpiryTrackingService expiryTrackingService;

    @Autowired
    private UserRepository userRepository;
    
    /**
     * Create expiry tracking
     */
    @PostMapping
    public ResponseEntity<ExpiryTracking> createExpiryTracking(
            @RequestBody CreateExpiryTrackingRequest request,
            Authentication authentication) {
        
        try {
            User user = resolveCurrentUser(authentication);
            
            // For now, we'll create tracking without validating document existence
            // In production, you should validate that the document exists
            
            ExpiryTracking tracking = expiryTrackingService.createExpiryTracking(
                null, // Document will be set by service
                request.getExpiryType(),
                request.getExpiryDate(),
                user
            );
            
            return ResponseEntity.ok(tracking);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Update expiry tracking
     */
    @PutMapping("/{trackingId}")
    public ResponseEntity<ExpiryTracking> updateExpiryTracking(
            @PathVariable Long trackingId,
            @RequestBody ExpiryTracking updatedTracking,
            Authentication authentication) {
        
        try {
            ExpiryTracking tracking = expiryTrackingService.updateExpiryTracking(trackingId, updatedTracking);
            
            if (tracking != null) {
                return ResponseEntity.ok(tracking);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Renew expiry tracking
     */
    @PostMapping("/{trackingId}/renew")
    public ResponseEntity<ExpiryTracking> renewExpiryTracking(
            @PathVariable Long trackingId,
            @RequestBody RenewExpiryTrackingRequest request,
            Authentication authentication) {
        
        try {
            ExpiryTracking tracking = expiryTrackingService.renewExpiryTracking(
                trackingId,
                request.getNewExpiryDate(),
                request.getRenewalDocumentId(),
                request.getNotes()
            );
            
            if (tracking != null) {
                return ResponseEntity.ok(tracking);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get expiry tracking by document
     */
    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<ExpiryTracking>> getExpiryTrackingByDocument(@PathVariable Long documentId) {
        try {
            List<ExpiryTracking> tracking = expiryTrackingService.getExpiryTrackingByDocument(documentId);
            return ResponseEntity.ok(tracking);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get active expiry tracking
     */
    @GetMapping("/active")
    public ResponseEntity<Page<ExpiryTracking>> getActiveExpiryTracking(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ExpiryTracking> tracking = expiryTrackingService.getActiveExpiryTracking(pageable);
            
            return ResponseEntity.ok(tracking);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get expiring documents
     */
    @GetMapping("/expiring")
    public ResponseEntity<List<ExpiryTracking>> getExpiringDocuments(
            @RequestParam(defaultValue = "30") int days) {
        
        try {
            List<ExpiryTracking> tracking = expiryTrackingService.getExpiringDocuments(days);
            return ResponseEntity.ok(tracking);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get expired documents
     */
    @GetMapping("/expired")
    public ResponseEntity<List<ExpiryTracking>> getExpiredDocuments() {
        try {
            List<ExpiryTracking> tracking = expiryTrackingService.getExpiredDocuments();
            return ResponseEntity.ok(tracking);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get expiry statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getExpiryStatistics() {
        try {
            Map<String, Object> statistics = expiryTrackingService.getExpiryStatistics();
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get Performance Security documents with expiry dates from metadata
     */
    @GetMapping("/performance-security")
    public ResponseEntity<List<Map<String, Object>>> getPerformanceSecurityDocuments() {
        try {
            List<Map<String, Object>> documents = expiryTrackingService.getPerformanceSecurityDocumentsWithExpiry();
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Create expiry tracking request DTO
     */
    public static class CreateExpiryTrackingRequest {
        private Long documentId;
        private ExpiryType expiryType;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime expiryDate;
        private String notes;
        
        // Getters and setters
        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }
        public ExpiryType getExpiryType() { return expiryType; }
        public void setExpiryType(ExpiryType expiryType) { this.expiryType = expiryType; }
        public LocalDateTime getExpiryDate() { return expiryDate; }
        public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
    
    /**
     * Renew expiry tracking request DTO
     */
    public static class RenewExpiryTrackingRequest {
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime newExpiryDate;
        private Long renewalDocumentId;
        private String notes;
        
        // Getters and setters
        public LocalDateTime getNewExpiryDate() { return newExpiryDate; }
        public void setNewExpiryDate(LocalDateTime newExpiryDate) { this.newExpiryDate = newExpiryDate; }
        public Long getRenewalDocumentId() { return renewalDocumentId; }
        public void setRenewalDocumentId(Long renewalDocumentId) { this.renewalDocumentId = renewalDocumentId; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    private User resolveCurrentUser(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("Missing authentication context");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + authentication.getName()));
    }
}
