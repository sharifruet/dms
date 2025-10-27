package com.bpdb.dms.service;

import com.bpdb.dms.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending SMS notifications
 */
@Service
public class SmsNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(SmsNotificationService.class);
    
    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;
    
    @Value("${app.sms.provider:twilio}")
    private String smsProvider;
    
    @Value("${app.sms.api.url:}")
    private String smsApiUrl;
    
    @Value("${app.sms.api.key:}")
    private String smsApiKey;
    
    @Value("${app.sms.api.secret:}")
    private String smsApiSecret;
    
    @Value("${app.sms.from:+1234567890}")
    private String smsFrom;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Send notification via SMS
     */
    public void sendNotification(Notification notification) {
        if (!smsEnabled) {
            logger.info("SMS notifications are disabled");
            return;
        }
        
        try {
            String userPhone = notification.getUser().getPhone();
            if (userPhone == null || userPhone.trim().isEmpty()) {
                logger.warn("User {} has no phone number", notification.getUser().getUsername());
                return;
            }
            
            // Format SMS message
            String message = formatSmsMessage(notification);
            
            // Send SMS based on provider
            switch (smsProvider.toLowerCase()) {
                case "twilio":
                    sendTwilioSms(userPhone, message);
                    break;
                case "custom":
                    sendCustomSms(userPhone, message);
                    break;
                default:
                    logger.warn("Unknown SMS provider: {}", smsProvider);
            }
            
            logger.info("SMS notification sent to {} for notification {}", userPhone, notification.getId());
            
        } catch (Exception e) {
            logger.error("Failed to send SMS notification {}: {}", notification.getId(), e.getMessage());
            throw new RuntimeException("Failed to send SMS notification", e);
        }
    }
    
    /**
     * Format SMS message for notification
     */
    private String formatSmsMessage(Notification notification) {
        StringBuilder message = new StringBuilder();
        message.append("DMS Alert: ");
        message.append(notification.getTitle()).append(" - ");
        message.append(notification.getMessage());
        
        // Truncate if too long (SMS limit is typically 160 characters)
        if (message.length() > 160) {
            message.setLength(157);
            message.append("...");
        }
        
        return message.toString();
    }
    
    /**
     * Send SMS via Twilio
     */
    private void sendTwilioSms(String to, String message) {
        try {
            // Twilio API implementation
            String twilioUrl = "https://api.twilio.com/2010-04-01/Accounts/" + smsApiKey + "/Messages.json";
            
            Map<String, String> params = new HashMap<>();
            params.put("From", smsFrom);
            params.put("To", to);
            params.put("Body", message);
            
            // Note: In production, use proper Twilio SDK
            logger.info("SMS would be sent via Twilio to {}: {}", to, message);
            
        } catch (Exception e) {
            logger.error("Failed to send Twilio SMS to {}: {}", to, e.getMessage());
        }
    }
    
    /**
     * Send SMS via custom API
     */
    private void sendCustomSms(String to, String message) {
        try {
            if (smsApiUrl == null || smsApiUrl.trim().isEmpty()) {
                logger.warn("SMS API URL not configured");
                return;
            }
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("to", to);
            requestBody.put("message", message);
            requestBody.put("from", smsFrom);
            
            // Add authentication headers
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + smsApiSecret);
            headers.put("Content-Type", "application/json");
            
            // Note: In production, implement proper HTTP client with headers
            logger.info("SMS would be sent via custom API to {}: {}", to, message);
            
        } catch (Exception e) {
            logger.error("Failed to send custom SMS to {}: {}", to, e.getMessage());
        }
    }
    
    /**
     * Send expiry alert SMS
     */
    public void sendExpiryAlert(String phone, String documentName, String expiryType, String daysRemaining) {
        if (!smsEnabled) {
            return;
        }
        
        try {
            String message = String.format("DMS Alert: %s (%s) expires in %s days. Please renew.", 
                documentName, expiryType, daysRemaining);
            
            switch (smsProvider.toLowerCase()) {
                case "twilio":
                    sendTwilioSms(phone, message);
                    break;
                case "custom":
                    sendCustomSms(phone, message);
                    break;
            }
            
        } catch (Exception e) {
            logger.error("Failed to send expiry alert SMS to {}: {}", phone, e.getMessage());
        }
    }
    
    /**
     * Send compliance alert SMS
     */
    public void sendComplianceAlert(String phone, String alertType) {
        if (!smsEnabled) {
            return;
        }
        
        try {
            String message = String.format("DMS Compliance Alert: %s requires attention. Please check system.", alertType);
            
            switch (smsProvider.toLowerCase()) {
                case "twilio":
                    sendTwilioSms(phone, message);
                    break;
                case "custom":
                    sendCustomSms(phone, message);
                    break;
            }
            
        } catch (Exception e) {
            logger.error("Failed to send compliance alert SMS to {}: {}", phone, e.getMessage());
        }
    }
    
    /**
     * Send system alert SMS
     */
    public void sendSystemAlert(String phone, String alertType) {
        if (!smsEnabled) {
            return;
        }
        
        try {
            String message = String.format("DMS System Alert: %s - Immediate attention required.", alertType);
            
            switch (smsProvider.toLowerCase()) {
                case "twilio":
                    sendTwilioSms(phone, message);
                    break;
                case "custom":
                    sendCustomSms(phone, message);
                    break;
            }
            
        } catch (Exception e) {
            logger.error("Failed to send system alert SMS to {}: {}", phone, e.getMessage());
        }
    }
    
    /**
     * Send bulk SMS notifications
     */
    public void sendBulkSms(String[] recipients, String message) {
        if (!smsEnabled) {
            return;
        }
        
        try {
            for (String recipient : recipients) {
                switch (smsProvider.toLowerCase()) {
                    case "twilio":
                        sendTwilioSms(recipient, message);
                        break;
                    case "custom":
                        sendCustomSms(recipient, message);
                        break;
                }
            }
            
            logger.info("Bulk SMS notifications sent to {} recipients", recipients.length);
            
        } catch (Exception e) {
            logger.error("Failed to send bulk SMS notifications: {}", e.getMessage());
        }
    }
    
    /**
     * Validate phone number format
     */
    public boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        
        // Basic phone number validation (adjust based on requirements)
        String cleaned = phone.replaceAll("[^0-9+]", "");
        return cleaned.length() >= 10 && cleaned.length() <= 15;
    }
}
