package com.bpdb.dms.service;

import com.bpdb.dms.entity.ReportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Service for generating Word reports
 */
@Service
public class WordReportService {
    
    private static final Logger logger = LoggerFactory.getLogger(WordReportService.class);
    
    @Value("${app.reports.directory:/app/reports}")
    private String reportsDirectory;
    
    /**
     * Generate Word report
     */
    public String generateReport(ReportType reportType, Map<String, Object> parameters) {
        try {
            String fileName = generateFileName(reportType);
            String filePath = reportsDirectory + "/" + fileName;
            
            // Create reports directory if it doesn't exist
            File directory = new File(reportsDirectory);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // Generate Word content
            byte[] wordContent = generateWordContent(reportType, parameters);
            
            // Write to file
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(wordContent);
            }
            
            logger.info("Word report generated: {}", filePath);
            return filePath;
            
        } catch (Exception e) {
            logger.error("Failed to generate Word report: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Generate Word content based on report type
     */
    private byte[] generateWordContent(ReportType reportType, Map<String, Object> parameters) {
        try {
            // This is a simplified implementation
            // In production, use Apache POI library for proper Word generation
            
            StringBuilder content = new StringBuilder();
            
            // Word document header
            content.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
            content.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n");
            content.append("<w:body>\n");
            
            // Add report-specific content
            content.append(generateReportContent(reportType, parameters));
            
            content.append("</w:body>\n");
            content.append("</w:document>\n");
            
            return content.toString().getBytes();
            
        } catch (Exception e) {
            logger.error("Failed to generate Word content: {}", e.getMessage());
            return new byte[0];
        }
    }
    
    /**
     * Generate report content
     */
    private String generateReportContent(ReportType reportType, Map<String, Object> parameters) {
        StringBuilder content = new StringBuilder();
        
        // Title
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:rPr>\n");
        content.append("<w:b/>\n");
        content.append("<w:sz w:val=\"24\"/>\n");
        content.append("</w:rPr>\n");
        content.append("<w:t>DMS Report - ").append(reportType.getDisplayName()).append("</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        // Date
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:t>Generated on: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        // Empty paragraph
        content.append("<w:p></w:p>\n");
        
        // Report-specific content
        switch (reportType) {
            case DOCUMENT_SUMMARY:
                content.append(generateDocumentSummaryContent(parameters));
                break;
            case USER_ACTIVITY:
                content.append(generateUserActivityContent(parameters));
                break;
            case EXPIRY_REPORT:
                content.append(generateExpiryReportContent(parameters));
                break;
            case SYSTEM_PERFORMANCE:
                content.append(generateSystemPerformanceContent(parameters));
                break;
            default:
                content.append(generateCustomReportContent(parameters));
                break;
        }
        
        return content.toString();
    }
    
    /**
     * Generate document summary content
     */
    private String generateDocumentSummaryContent(Map<String, Object> parameters) {
        StringBuilder content = new StringBuilder();
        
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:rPr>\n");
        content.append("<w:b/>\n");
        content.append("</w:rPr>\n");
        content.append("<w:t>Document Summary</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:t>Total Documents: ").append(parameters.getOrDefault("totalDocuments", "N/A")).append("</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:t>Active Documents: ").append(parameters.getOrDefault("activeDocuments", "N/A")).append("</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:t>Recent Uploads (30 days): ").append(parameters.getOrDefault("recentUploads", "N/A")).append("</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        return content.toString();
    }
    
    /**
     * Generate user activity content
     */
    private String generateUserActivityContent(Map<String, Object> parameters) {
        StringBuilder content = new StringBuilder();
        
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:rPr>\n");
        content.append("<w:b/>\n");
        content.append("</w:rPr>\n");
        content.append("<w:t>User Activity Summary</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:t>Total Users: ").append(parameters.getOrDefault("totalUsers", "N/A")).append("</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:t>Active Users: ").append(parameters.getOrDefault("activeUsers", "N/A")).append("</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:t>Recent Logins (30 days): ").append(parameters.getOrDefault("recentLogins", "N/A")).append("</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        return content.toString();
    }
    
    /**
     * Generate expiry report content
     */
    private String generateExpiryReportContent(Map<String, Object> parameters) {
        StringBuilder content = new StringBuilder();
        
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:rPr>\n");
        content.append("<w:b/>\n");
        content.append("</w:rPr>\n");
        content.append("<w:t>Expiry Report</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:t>Active Tracking: ").append(parameters.getOrDefault("activeTracking", "N/A")).append("</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:t>Expired Documents: ").append(parameters.getOrDefault("expiredDocuments", "N/A")).append("</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:t>Expiring in 30 days: ").append(parameters.getOrDefault("expiringIn30Days", "N/A")).append("</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:t>Expiring in 7 days: ").append(parameters.getOrDefault("expiringIn7Days", "N/A")).append("</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        return content.toString();
    }
    
    /**
     * Generate system performance content
     */
    private String generateSystemPerformanceContent(Map<String, Object> parameters) {
        StringBuilder content = new StringBuilder();
        
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:rPr>\n");
        content.append("<w:b/>\n");
        content.append("</w:rPr>\n");
        content.append("<w:t>System Performance Report</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:t>Estimated Storage: ").append(parameters.getOrDefault("estimatedStorageMB", "N/A")).append(" MB</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:t>Uptime: ").append(parameters.getOrDefault("uptimeHours", "N/A")).append(" hours</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:t>Average Response Time: ").append(parameters.getOrDefault("averageResponseTimeMs", "N/A")).append(" ms</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:t>Error Rate: ").append(parameters.getOrDefault("errorRate", "N/A")).append("</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        return content.toString();
    }
    
    /**
     * Generate custom report content
     */
    private String generateCustomReportContent(Map<String, Object> parameters) {
        StringBuilder content = new StringBuilder();
        
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:rPr>\n");
        content.append("<w:b/>\n");
        content.append("</w:rPr>\n");
        content.append("<w:t>Custom Report</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            content.append("<w:p>\n");
            content.append("<w:r>\n");
            content.append("<w:t>").append(entry.getKey()).append(": ").append(entry.getValue()).append("</w:t>\n");
            content.append("</w:r>\n");
            content.append("</w:p>\n");
        }
        
        return content.toString();
    }
    
    /**
     * Generate file name for report
     */
    private String generateFileName(ReportType reportType) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return reportType.name().toLowerCase() + "_" + timestamp + ".docx";
    }
    
    /**
     * Generate compliance report Word document
     */
    public String generateComplianceReport(Map<String, Object> data) {
        try {
            String fileName = "compliance_report_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".docx";
            String filePath = reportsDirectory + "/" + fileName;
            
            // Create reports directory if it doesn't exist
            File directory = new File(reportsDirectory);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // Generate compliance report content
            byte[] wordContent = generateComplianceReportContent(data);
            
            // Write to file
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(wordContent);
            }
            
            logger.info("Compliance report Word generated: {}", filePath);
            return filePath;
            
        } catch (Exception e) {
            logger.error("Failed to generate compliance report Word: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Generate compliance report content
     */
    private byte[] generateComplianceReportContent(Map<String, Object> data) {
        StringBuilder content = new StringBuilder();
        
        content.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        content.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n");
        content.append("<w:body>\n");
        
        // Title
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:rPr>\n");
        content.append("<w:b/>\n");
        content.append("<w:sz w:val=\"24\"/>\n");
        content.append("</w:rPr>\n");
        content.append("<w:t>Compliance Report</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        // Date
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:t>Generated on: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        content.append("<w:p></w:p>\n");
        
        // Compliance summary
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:rPr>\n");
        content.append("<w:b/>\n");
        content.append("</w:rPr>\n");
        content.append("<w:t>Compliance Summary</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:t>This report provides an overview of system compliance with organizational policies and procedures.</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        content.append("<w:p>\n");
        content.append("<w:r>\n");
        content.append("<w:t>Key compliance metrics and recommendations are included for management review.</w:t>\n");
        content.append("</w:r>\n");
        content.append("</w:p>\n");
        
        content.append("</w:body>\n");
        content.append("</w:document>\n");
        
        return content.toString().getBytes();
    }
}
