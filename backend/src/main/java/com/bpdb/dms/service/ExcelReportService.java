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
import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating Excel reports
 */
@Service
public class ExcelReportService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExcelReportService.class);
    
    @Value("${app.reports.directory:/app/reports}")
    private String reportsDirectory;
    
    /**
     * Generate Excel report
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
            
            // Generate Excel content
            byte[] excelContent = generateExcelContent(reportType, parameters);
            
            // Write to file
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(excelContent);
            }
            
            logger.info("Excel report generated: {}", filePath);
            return filePath;
            
        } catch (Exception e) {
            logger.error("Failed to generate Excel report: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Generate Excel content based on report type
     */
    private byte[] generateExcelContent(ReportType reportType, Map<String, Object> parameters) {
        try {
            // This is a simplified implementation
            // In production, use Apache POI library for proper Excel generation
            
            StringBuilder content = new StringBuilder();
            
            // Excel file header
            content.append("<?xml version=\"1.0\"?>\n");
            content.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
            content.append(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n");
            content.append(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n");
            content.append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
            content.append(" xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n");
            
            // Worksheet
            content.append("<Worksheet ss:Name=\"").append(reportType.getDisplayName()).append("\">\n");
            content.append("<Table>\n");
            
            // Add report-specific content
            content.append(generateReportRows(reportType, parameters));
            
            content.append("</Table>\n");
            content.append("</Worksheet>\n");
            content.append("</Workbook>\n");
            
            return content.toString().getBytes();
            
        } catch (Exception e) {
            logger.error("Failed to generate Excel content: {}", e.getMessage());
            return new byte[0];
        }
    }
    
    /**
     * Generate report rows
     */
    private String generateReportRows(ReportType reportType, Map<String, Object> parameters) {
        StringBuilder rows = new StringBuilder();
        
        // Header row
        rows.append("<Row>\n");
        rows.append("<Cell><Data ss:Type=\"String\">DMS Report - ").append(reportType.getDisplayName()).append("</Data></Cell>\n");
        rows.append("</Row>\n");
        
        // Date row
        rows.append("<Row>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Generated on: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</Data></Cell>\n");
        rows.append("</Row>\n");
        
        // Empty row
        rows.append("<Row></Row>\n");
        
        // Report-specific data
        switch (reportType) {
            case DOCUMENT_SUMMARY:
                rows.append(generateDocumentSummaryRows(parameters));
                break;
            case USER_ACTIVITY:
                rows.append(generateUserActivityRows(parameters));
                break;
            case EXPIRY_REPORT:
                rows.append(generateExpiryReportRows(parameters));
                break;
            case SYSTEM_PERFORMANCE:
                rows.append(generateSystemPerformanceRows(parameters));
                break;
            default:
                rows.append(generateCustomReportRows(parameters));
                break;
        }
        
        return rows.toString();
    }
    
    /**
     * Generate document summary rows
     */
    private String generateDocumentSummaryRows(Map<String, Object> parameters) {
        StringBuilder rows = new StringBuilder();
        
        rows.append("<Row>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Metric</Data></Cell>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Value</Data></Cell>\n");
        rows.append("</Row>\n");
        
        rows.append("<Row>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Total Documents</Data></Cell>\n");
        rows.append("<Cell><Data ss:Type=\"Number\">").append(parameters.getOrDefault("totalDocuments", 0)).append("</Data></Cell>\n");
        rows.append("</Row>\n");
        
        rows.append("<Row>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Active Documents</Data></Cell>\n");
        rows.append("<Cell><Data ss:Type=\"Number\">").append(parameters.getOrDefault("activeDocuments", 0)).append("</Data></Cell>\n");
        rows.append("</Row>\n");
        
        rows.append("<Row>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Recent Uploads (30 days)</Data></Cell>\n");
        rows.append("<Cell><Data ss:Type=\"Number\">").append(parameters.getOrDefault("recentUploads", 0)).append("</Data></Cell>\n");
        rows.append("</Row>\n");
        
        return rows.toString();
    }
    
    /**
     * Generate user activity rows
     */
    private String generateUserActivityRows(Map<String, Object> parameters) {
        StringBuilder rows = new StringBuilder();
        
        rows.append("<Row>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Metric</Data></Cell>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Value</Data></Cell>\n");
        rows.append("</Row>\n");
        
        rows.append("<Row>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Total Users</Data></Cell>\n");
        rows.append("<Cell><Data ss:Type=\"Number\">").append(parameters.getOrDefault("totalUsers", 0)).append("</Data></Cell>\n");
        rows.append("</Row>\n");
        
        rows.append("<Row>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Active Users</Data></Cell>\n");
        rows.append("<Cell><Data ss:Type=\"Number\">").append(parameters.getOrDefault("activeUsers", 0)).append("</Data></Cell>\n");
        rows.append("</Row>\n");
        
        rows.append("<Row>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Recent Logins (30 days)</Data></Cell>\n");
        rows.append("<Cell><Data ss:Type=\"Number\">").append(parameters.getOrDefault("recentLogins", 0)).append("</Data></Cell>\n");
        rows.append("</Row>\n");
        
        return rows.toString();
    }
    
    /**
     * Generate expiry report rows
     */
    private String generateExpiryReportRows(Map<String, Object> parameters) {
        StringBuilder rows = new StringBuilder();
        
        rows.append("<Row>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Metric</Data></Cell>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Value</Data></Cell>\n");
        rows.append("</Row>\n");
        
        rows.append("<Row>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Active Tracking</Data></Cell>\n");
        rows.append("<Cell><Data ss:Type=\"Number\">").append(parameters.getOrDefault("activeTracking", 0)).append("</Data></Cell>\n");
        rows.append("</Row>\n");
        
        rows.append("<Row>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Expired Documents</Data></Cell>\n");
        rows.append("<Cell><Data ss:Type=\"Number\">").append(parameters.getOrDefault("expiredDocuments", 0)).append("</Data></Cell>\n");
        rows.append("</Row>\n");
        
        rows.append("<Row>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Expiring in 30 days</Data></Cell>\n");
        rows.append("<Cell><Data ss:Type=\"Number\">").append(parameters.getOrDefault("expiringIn30Days", 0)).append("</Data></Cell>\n");
        rows.append("</Row>\n");
        
        rows.append("<Row>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Expiring in 7 days</Data></Cell>\n");
        rows.append("<Cell><Data ss:Type=\"Number\">").append(parameters.getOrDefault("expiringIn7Days", 0)).append("</Data></Cell>\n");
        rows.append("</Row>\n");
        
        return rows.toString();
    }
    
    /**
     * Generate system performance rows
     */
    private String generateSystemPerformanceRows(Map<String, Object> parameters) {
        StringBuilder rows = new StringBuilder();
        
        rows.append("<Row>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Metric</Data></Cell>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Value</Data></Cell>\n");
        rows.append("</Row>\n");
        
        rows.append("<Row>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Estimated Storage (MB)</Data></Cell>\n");
        rows.append("<Cell><Data ss:Type=\"Number\">").append(parameters.getOrDefault("estimatedStorageMB", 0)).append("</Data></Cell>\n");
        rows.append("</Row>\n");
        
        rows.append("<Row>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Uptime (hours)</Data></Cell>\n");
        rows.append("<Cell><Data ss:Type=\"Number\">").append(parameters.getOrDefault("uptimeHours", 0)).append("</Data></Cell>\n");
        rows.append("</Row>\n");
        
        rows.append("<Row>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Average Response Time (ms)</Data></Cell>\n");
        rows.append("<Cell><Data ss:Type=\"Number\">").append(parameters.getOrDefault("averageResponseTimeMs", 0)).append("</Data></Cell>\n");
        rows.append("</Row>\n");
        
        rows.append("<Row>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Error Rate</Data></Cell>\n");
        rows.append("<Cell><Data ss:Type=\"Number\">").append(parameters.getOrDefault("errorRate", 0)).append("</Data></Cell>\n");
        rows.append("</Row>\n");
        
        return rows.toString();
    }
    
    /**
     * Generate custom report rows
     */
    private String generateCustomReportRows(Map<String, Object> parameters) {
        StringBuilder rows = new StringBuilder();
        
        rows.append("<Row>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Parameter</Data></Cell>\n");
        rows.append("<Cell><Data ss:Type=\"String\">Value</Data></Cell>\n");
        rows.append("</Row>\n");
        
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            rows.append("<Row>\n");
            rows.append("<Cell><Data ss:Type=\"String\">").append(entry.getKey()).append("</Data></Cell>\n");
            rows.append("<Cell><Data ss:Type=\"String\">").append(entry.getValue()).append("</Data></Cell>\n");
            rows.append("</Row>\n");
        }
        
        return rows.toString();
    }
    
    /**
     * Generate file name for report
     */
    private String generateFileName(ReportType reportType) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return reportType.name().toLowerCase() + "_" + timestamp + ".xls";
    }
    
    /**
     * Generate department-wise summary Excel
     */
    public String generateDepartmentSummary(Map<String, Object> data) {
        try {
            String fileName = "department_summary_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xls";
            String filePath = reportsDirectory + "/" + fileName;
            
            // Create reports directory if it doesn't exist
            File directory = new File(reportsDirectory);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // Generate department summary content
            byte[] excelContent = generateDepartmentSummaryContent(data);
            
            // Write to file
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(excelContent);
            }
            
            logger.info("Department summary Excel generated: {}", filePath);
            return filePath;
            
        } catch (Exception e) {
            logger.error("Failed to generate department summary Excel: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Generate department summary content
     */
    private byte[] generateDepartmentSummaryContent(Map<String, Object> data) {
        StringBuilder content = new StringBuilder();
        
        content.append("<?xml version=\"1.0\"?>\n");
        content.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\">\n");
        content.append("<Worksheet ss:Name=\"Department Summary\">\n");
        content.append("<Table>\n");
        
        // Header
        content.append("<Row>\n");
        content.append("<Cell><Data ss:Type=\"String\">Department Summary Report</Data></Cell>\n");
        content.append("</Row>\n");
        
        content.append("<Row>\n");
        content.append("<Cell><Data ss:Type=\"String\">Generated on: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</Data></Cell>\n");
        content.append("</Row>\n");
        
        content.append("<Row></Row>\n");
        
        // Department data
        content.append("<Row>\n");
        content.append("<Cell><Data ss:Type=\"String\">Department</Data></Cell>\n");
        content.append("<Cell><Data ss:Type=\"String\">Document Count</Data></Cell>\n");
        content.append("<Cell><Data ss:Type=\"String\">User Count</Data></Cell>\n");
        content.append("</Row>\n");
        
        // Add department data rows
        Map<String, Long> documentsByDept = (Map<String, Long>) data.getOrDefault("documentsByDepartment", new HashMap<>());
        Map<String, Long> usersByDept = (Map<String, Long>) data.getOrDefault("usersByDepartment", new HashMap<>());
        
        for (String dept : documentsByDept.keySet()) {
            content.append("<Row>\n");
            content.append("<Cell><Data ss:Type=\"String\">").append(dept).append("</Data></Cell>\n");
            content.append("<Cell><Data ss:Type=\"Number\">").append(documentsByDept.get(dept)).append("</Data></Cell>\n");
            content.append("<Cell><Data ss:Type=\"Number\">").append(usersByDept.getOrDefault(dept, 0L)).append("</Data></Cell>\n");
            content.append("</Row>\n");
        }
        
        content.append("</Table>\n");
        content.append("</Worksheet>\n");
        content.append("</Workbook>\n");
        
        return content.toString().getBytes();
    }
}
