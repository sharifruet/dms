package com.bpdb.dms.service;

import com.bpdb.dms.entity.*;
import com.bpdb.dms.repository.WebhookRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing webhooks
 */
@Service
@Transactional
public class WebhookService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    
    @Autowired
    private WebhookRepository webhookRepository;
    
    @Autowired
    private AuditService auditService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Create a new webhook
     */
    public Webhook createWebhook(String name, String description, String url, 
                               WebhookEventType eventType, User createdBy) {
        try {
            Webhook webhook = new Webhook(name, description, url, eventType, createdBy);
            
            Webhook savedWebhook = webhookRepository.save(webhook);
            
            auditService.logActivity(createdBy.getUsername(), "WEBHOOK_CREATED", 
                "Webhook created: " + name, null);
            
            logger.info("Webhook created: {} by user: {}", name, createdBy.getUsername());
            
            return savedWebhook;
            
        } catch (Exception e) {
            logger.error("Failed to create webhook: {}", e.getMessage());
            throw new RuntimeException("Failed to create webhook", e);
        }
    }
    
    /**
     * Update webhook
     */
    public Webhook updateWebhook(Long webhookId, String name, String description, String url, 
                               WebhookEventType eventType, Boolean isEnabled) {
        try {
            Webhook webhook = webhookRepository.findById(webhookId)
                .orElseThrow(() -> new RuntimeException("Webhook not found"));
            
            webhook.setName(name);
            webhook.setDescription(description);
            webhook.setUrl(url);
            webhook.setEventType(eventType);
            webhook.setIsEnabled(isEnabled);
            
            Webhook savedWebhook = webhookRepository.save(webhook);
            
            auditService.logActivity("SYSTEM", "WEBHOOK_UPDATED", 
                "Webhook updated: " + name, null);
            
            logger.info("Webhook updated: {}", name);
            
            return savedWebhook;
            
        } catch (Exception e) {
            logger.error("Failed to update webhook: {}", e.getMessage());
            throw new RuntimeException("Failed to update webhook", e);
        }
    }
    
    /**
     * Delete webhook
     */
    public void deleteWebhook(Long webhookId) {
        try {
            Webhook webhook = webhookRepository.findById(webhookId)
                .orElseThrow(() -> new RuntimeException("Webhook not found"));
            
            webhookRepository.delete(webhook);
            
            auditService.logActivity("SYSTEM", "WEBHOOK_DELETED", 
                "Webhook deleted: " + webhook.getName(), null);
            
            logger.info("Webhook deleted: {}", webhook.getName());
            
        } catch (Exception e) {
            logger.error("Failed to delete webhook: {}", e.getMessage());
            throw new RuntimeException("Failed to delete webhook", e);
        }
    }
    
    /**
     * Trigger webhook for an event
     */
    @Async
    public CompletableFuture<Void> triggerWebhook(WebhookEventType eventType, Object eventData) {
        try {
            List<Webhook> webhooks = webhookRepository.findByEventTypeAndIsEnabledTrue(eventType);
            
            for (Webhook webhook : webhooks) {
                sendWebhookAsync(webhook, eventData);
            }
            
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            logger.error("Failed to trigger webhook: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Send webhook asynchronously
     */
    @Async
    public CompletableFuture<Void> sendWebhookAsync(Webhook webhook, Object eventData) {
        try {
            String payload = createWebhookPayload(webhook, eventData);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("User-Agent", "DMS-Webhook/1.0");
            
            // Add custom headers if specified
            if (webhook.getHeaders() != null && !webhook.getHeaders().isEmpty()) {
                try {
                    Map<String, String> customHeaders = objectMapper.readValue(webhook.getHeaders(), Map.class);
                    customHeaders.forEach(headers::set);
                } catch (JsonProcessingException e) {
                    logger.warn("Failed to parse custom headers for webhook: {}", webhook.getName());
                }
            }
            
            // Add signature if secret key is provided
            if (webhook.getSecretKey() != null && !webhook.getSecretKey().isEmpty()) {
                String signature = generateSignature(payload, webhook.getSecretKey());
                headers.set("X-Webhook-Signature", signature);
            }
            
            HttpEntity<String> request = new HttpEntity<>(payload, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                webhook.getUrl(),
                HttpMethod.POST,
                request,
                String.class
            );
            
            // Update webhook statistics
            webhook.setLastTriggeredAt(LocalDateTime.now());
            webhook.setSuccessCount(webhook.getSuccessCount() + 1);
            webhook.setLastError(null);
            webhookRepository.save(webhook);
            
            logger.info("Webhook sent successfully: {} - Status: {}", 
                webhook.getName(), response.getStatusCode());
            
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            // Update webhook statistics
            webhook.setLastTriggeredAt(LocalDateTime.now());
            webhook.setFailureCount(webhook.getFailureCount() + 1);
            webhook.setLastError(e.getMessage());
            webhookRepository.save(webhook);
            
            logger.error("Failed to send webhook: {} - Error: {}", 
                webhook.getName(), e.getMessage());
            
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Test webhook
     */
    public boolean testWebhook(Long webhookId) {
        try {
            Webhook webhook = webhookRepository.findById(webhookId)
                .orElseThrow(() -> new RuntimeException("Webhook not found"));
            
            Map<String, Object> testData = Map.of(
                "event", "test",
                "timestamp", LocalDateTime.now().toString(),
                "message", "This is a test webhook from DMS"
            );
            
            CompletableFuture<Void> future = sendWebhookAsync(webhook, testData);
            future.get(); // Wait for completion
            
            return true;
            
        } catch (Exception e) {
            logger.error("Webhook test failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get webhook statistics
     */
    public Map<String, Object> getWebhookStatistics() {
        try {
            return Map.of(
                "totalWebhooks", webhookRepository.count(),
                "activeWebhooks", webhookRepository.countByStatus(WebhookStatus.ACTIVE),
                "enabledWebhooks", webhookRepository.countByIsEnabledTrue(),
                "inactiveWebhooks", webhookRepository.countByStatus(WebhookStatus.INACTIVE)
            );
            
        } catch (Exception e) {
            logger.error("Failed to get webhook statistics: {}", e.getMessage());
            throw new RuntimeException("Failed to get webhook statistics", e);
        }
    }
    
    /**
     * Get webhooks by event type
     */
    public List<Webhook> getWebhooksByEventType(WebhookEventType eventType) {
        return webhookRepository.findByEventType(eventType);
    }
    
    /**
     * Get webhooks for user
     */
    public Page<Webhook> getWebhooksForUser(User user, Pageable pageable) {
        return webhookRepository.findByCreatedBy(user, pageable);
    }
    
    /**
     * Create webhook payload
     */
    private String createWebhookPayload(Webhook webhook, Object eventData) {
        try {
            Map<String, Object> payload = Map.of(
                "webhookId", webhook.getId(),
                "webhookName", webhook.getName(),
                "eventType", webhook.getEventType().name(),
                "timestamp", LocalDateTime.now().toString(),
                "data", eventData
            );
            
            return objectMapper.writeValueAsString(payload);
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to create webhook payload: {}", e.getMessage());
            return "{}";
        }
    }
    
    /**
     * Generate webhook signature
     */
    private String generateSignature(String payload, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                secret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] signature = mac.doFinal(payload.getBytes());
            return "sha256=" + bytesToHex(signature);
            
        } catch (Exception e) {
            logger.error("Failed to generate webhook signature: {}", e.getMessage());
            return "";
        }
    }
    
    /**
     * Convert bytes to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
