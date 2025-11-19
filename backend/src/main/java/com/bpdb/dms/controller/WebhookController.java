package com.bpdb.dms.controller;

import com.bpdb.dms.entity.*;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.WebhookService;
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
 * Controller for webhook management
 */
@RestController
@RequestMapping("/api/webhooks")
@CrossOrigin(origins = "*")
public class WebhookController {
    
    @Autowired
    private WebhookService webhookService;

    @Autowired
    private UserRepository userRepository;
    
    /**
     * Create a new webhook
     */
    @PostMapping
    public ResponseEntity<Webhook> createWebhook(@RequestBody CreateWebhookRequest request,
                                                Authentication authentication) {
        try {
            User user = resolveCurrentUser(authentication);
            
            Webhook webhook = webhookService.createWebhook(
                request.getName(),
                request.getDescription(),
                request.getUrl(),
                request.getEventType(),
                user
            );
            
            return ResponseEntity.ok(webhook);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Update webhook
     */
    @PutMapping("/{webhookId}")
    public ResponseEntity<Webhook> updateWebhook(@PathVariable Long webhookId,
                                               @RequestBody UpdateWebhookRequest request) {
        try {
            Webhook webhook = webhookService.updateWebhook(
                webhookId,
                request.getName(),
                request.getDescription(),
                request.getUrl(),
                request.getEventType(),
                request.getIsEnabled()
            );
            
            return ResponseEntity.ok(webhook);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Delete webhook
     */
    @DeleteMapping("/{webhookId}")
    public ResponseEntity<Void> deleteWebhook(@PathVariable Long webhookId) {
        try {
            webhookService.deleteWebhook(webhookId);
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Test webhook
     */
    @PostMapping("/{webhookId}/test")
    public ResponseEntity<Boolean> testWebhook(@PathVariable Long webhookId) {
        try {
            boolean success = webhookService.testWebhook(webhookId);
            
            return ResponseEntity.ok(success);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get webhook statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getWebhookStatistics() {
        try {
            Map<String, Object> statistics = webhookService.getWebhookStatistics();
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get webhooks by event type
     */
    @GetMapping("/event-type/{eventType}")
    public ResponseEntity<List<Webhook>> getWebhooksByEventType(@PathVariable WebhookEventType eventType) {
        try {
            List<Webhook> webhooks = webhookService.getWebhooksByEventType(eventType);
            
            return ResponseEntity.ok(webhooks);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get webhooks for user
     */
    @GetMapping
    public ResponseEntity<Page<Webhook>> getWebhooksForUser(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        try {
            User user = resolveCurrentUser(authentication);
            Pageable pageable = PageRequest.of(page, size);
            
            Page<Webhook> webhooks = webhookService.getWebhooksForUser(user, pageable);
            
            return ResponseEntity.ok(webhooks);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Request DTOs
    public static class CreateWebhookRequest {
        private String name;
        private String description;
        private String url;
        private WebhookEventType eventType;
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public WebhookEventType getEventType() { return eventType; }
        public void setEventType(WebhookEventType eventType) { this.eventType = eventType; }
    }
    
    public static class UpdateWebhookRequest {
        private String name;
        private String description;
        private String url;
        private WebhookEventType eventType;
        private Boolean isEnabled;
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public WebhookEventType getEventType() { return eventType; }
        public void setEventType(WebhookEventType eventType) { this.eventType = eventType; }
        
        public Boolean getIsEnabled() { return isEnabled; }
        public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }
    }

    private User resolveCurrentUser(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("Missing authentication context");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + authentication.getName()));
    }
}
