package com.bpdb.dms.service;

import com.bpdb.dms.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending email notifications
 */
@Service
public class EmailNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private TemplateEngine templateEngine;
    
    @Value("${app.mail.from:noreply@dms.bpdb.gov.bd}")
    private String fromEmail;
    
    @Value("${app.mail.from.name:DMS System}")
    private String fromName;
    
    @Value("${app.mail.enabled:true}")
    private boolean emailEnabled;
    
    /**
     * Send notification via email
     */
    public void sendNotification(Notification notification) {
        if (!emailEnabled) {
            logger.info("Email notifications are disabled");
            return;
        }
        
        try {
            String userEmail = notification.getUser().getEmail();
            if (userEmail == null || userEmail.trim().isEmpty()) {
                logger.warn("User {} has no email address", notification.getUser().getUsername());
                return;
            }
            
            // Send HTML email
            sendHtmlEmail(notification);
            
            logger.info("Email notification sent to {} for notification {}", userEmail, notification.getId());
            
        } catch (Exception e) {
            logger.error("Failed to send email notification {}: {}", notification.getId(), e.getMessage());
            throw new RuntimeException("Failed to send email notification", e);
        }
    }
    
    /**
     * Send HTML email notification
     */
    private void sendHtmlEmail(Notification notification) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        try {
            helper.setFrom(fromEmail, fromName);
        } catch (Exception e) {
            logger.error("Error setting email from address: {}", e.getMessage());
        }
        helper.setTo(notification.getUser().getEmail());
        helper.setSubject(notification.getTitle());
        
        // Prepare template context
        Context context = new Context();
        context.setVariable("notification", notification);
        context.setVariable("user", notification.getUser());
        context.setVariable("appName", "DMS System");
        context.setVariable("appUrl", "http://localhost:3000");
        
        // Generate HTML content
        String htmlContent = templateEngine.process("email/notification", context);
        helper.setText(htmlContent, true);
        
        mailSender.send(message);
    }
    
    /**
     * Send simple text email
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        if (!emailEnabled) {
            logger.info("Email notifications are disabled");
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            
            logger.info("Simple email sent to {}", to);
            
        } catch (Exception e) {
            logger.error("Failed to send simple email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    /**
     * Send expiry alert email
     */
    public void sendExpiryAlert(String to, String documentName, String expiryType, 
                               String expiryDate, String daysRemaining) {
        if (!emailEnabled) {
            return;
        }
        
        try {
            String subject = String.format("Expiry Alert: %s - %s days remaining", documentName, daysRemaining);
            
            StringBuilder text = new StringBuilder();
            text.append("Dear User,\n\n");
            text.append("This is to inform you that the following document is expiring soon:\n\n");
            text.append("Document: ").append(documentName).append("\n");
            text.append("Type: ").append(expiryType).append("\n");
            text.append("Expiry Date: ").append(expiryDate).append("\n");
            text.append("Days Remaining: ").append(daysRemaining).append("\n\n");
            text.append("Please take necessary action to renew or extend the document.\n\n");
            text.append("Best regards,\n");
            text.append("DMS System");
            
            sendSimpleEmail(to, subject, text.toString());
            
        } catch (Exception e) {
            logger.error("Failed to send expiry alert email to {}: {}", to, e.getMessage());
        }
    }
    
    /**
     * Send compliance alert email
     */
    public void sendComplianceAlert(String to, String alertType, String message) {
        if (!emailEnabled) {
            return;
        }
        
        try {
            String subject = String.format("Compliance Alert: %s", alertType);
            
            StringBuilder text = new StringBuilder();
            text.append("Dear User,\n\n");
            text.append("A compliance alert has been triggered:\n\n");
            text.append("Alert Type: ").append(alertType).append("\n");
            text.append("Message: ").append(message).append("\n\n");
            text.append("Please review and take appropriate action.\n\n");
            text.append("Best regards,\n");
            text.append("DMS System");
            
            sendSimpleEmail(to, subject, text.toString());
            
        } catch (Exception e) {
            logger.error("Failed to send compliance alert email to {}: {}", to, e.getMessage());
        }
    }
    
    /**
     * Send system alert email
     */
    public void sendSystemAlert(String to, String alertType, String message) {
        if (!emailEnabled) {
            return;
        }
        
        try {
            String subject = String.format("System Alert: %s", alertType);
            
            StringBuilder text = new StringBuilder();
            text.append("Dear Administrator,\n\n");
            text.append("A system alert has been triggered:\n\n");
            text.append("Alert Type: ").append(alertType).append("\n");
            text.append("Message: ").append(message).append("\n");
            text.append("Time: ").append(java.time.LocalDateTime.now()).append("\n\n");
            text.append("Please investigate and take appropriate action.\n\n");
            text.append("Best regards,\n");
            text.append("DMS System");
            
            sendSimpleEmail(to, subject, text.toString());
            
        } catch (Exception e) {
            logger.error("Failed to send system alert email to {}: {}", to, e.getMessage());
        }
    }
    
    /**
     * Send bulk notifications
     */
    public void sendBulkNotifications(String[] recipients, String subject, String message) {
        if (!emailEnabled) {
            return;
        }
        
        try {
            for (String recipient : recipients) {
                sendSimpleEmail(recipient, subject, message);
            }
            
            logger.info("Bulk email notifications sent to {} recipients", recipients.length);
            
        } catch (Exception e) {
            logger.error("Failed to send bulk email notifications: {}", e.getMessage());
        }
    }
}
