package com.bpdb.dms.service;

import com.bpdb.dms.entity.*;
import com.bpdb.dms.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing dashboards and analytics
 */
@Service
@Transactional
public class DashboardService {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    
    @Autowired
    public DashboardRepository dashboardRepository;
    
    @Autowired
    private AnalyticsRepository analyticsRepository;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ExpiryTrackingRepository expiryTrackingRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private ReportingService reportingService;
    
    /**
     * Create a new dashboard
     */
    public Dashboard createDashboard(String name, String description, DashboardType type, 
                                   User createdBy, String layoutConfig, String widgetsConfig) {
        try {
            Dashboard dashboard = new Dashboard(name, description, type, createdBy);
            dashboard.setLayoutConfig(layoutConfig);
            dashboard.setWidgetsConfig(widgetsConfig);
            dashboard.setRefreshInterval(300); // 5 minutes default
            
            Dashboard savedDashboard = dashboardRepository.save(dashboard);
            
            logger.info("Dashboard created: {} by user {}", name, createdBy.getUsername());
            
            return savedDashboard;
            
        } catch (Exception e) {
            logger.error("Failed to create dashboard {}: {}", name, e.getMessage());
            throw new RuntimeException("Failed to create dashboard", e);
        }
    }
    
    /**
     * Get executive dashboard data
     */
    public Map<String, Object> getExecutiveDashboardData() {
        Map<String, Object> data = new HashMap<>();
        
        try {
            // Document metrics
            long totalDocuments = documentRepository.count();
            long activeDocuments = documentRepository.countByIsActiveTrue();
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            long recentUploads = documentRepository.countByCreatedAtAfter(thirtyDaysAgo);
            
            data.put("totalDocuments", totalDocuments);
            data.put("activeDocuments", activeDocuments);
            data.put("recentUploads", recentUploads);
            
            // User metrics
            long totalUsers = userRepository.count();
            long activeUsers = userRepository.countByIsActiveTrue();
            long recentLogins = auditLogRepository.countByCreatedAtAfterAndAction(thirtyDaysAgo, "LOGIN");
            
            data.put("totalUsers", totalUsers);
            data.put("activeUsers", activeUsers);
            data.put("recentLogins", recentLogins);
            
            // Expiry metrics
            long activeTracking = expiryTrackingRepository.countByStatus(ExpiryStatus.ACTIVE);
            long expiredDocuments = expiryTrackingRepository.countByStatus(ExpiryStatus.EXPIRED);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime thirtyDaysFromNow = now.plusDays(30);
            List<ExpiryTracking> expiringIn30Days = expiryTrackingRepository.findExpiringBetween(now, thirtyDaysFromNow);
            
            data.put("activeTracking", activeTracking);
            data.put("expiredDocuments", expiredDocuments);
            data.put("expiringIn30Days", expiringIn30Days.size());
            
            // Notification metrics
            long unreadNotifications = notificationRepository.countByStatus(NotificationStatus.PENDING);
            data.put("unreadNotifications", unreadNotifications);
            
            // System metrics
            data.put("systemUptime", 24 * 30); // 30 days placeholder
            data.put("averageResponseTime", 150); // ms placeholder
            data.put("errorRate", 0.01); // 1% placeholder
            
            // Storage metrics
            double estimatedStorage = totalDocuments * 0.5; // 0.5 MB per document average
            data.put("estimatedStorageMB", estimatedStorage);
            
        } catch (Exception e) {
            logger.error("Failed to get executive dashboard data: {}", e.getMessage());
        }
        
        return data;
    }
    
    /**
     * Get department dashboard data
     */
    public Map<String, Object> getDepartmentDashboardData(String department) {
        Map<String, Object> data = new HashMap<>();
        
        try {
            // Department-specific document metrics
            long deptDocuments = documentRepository.countByDepartment(department);
            long deptActiveDocuments = documentRepository.countByDepartmentAndIsActiveTrue(department);
            
            data.put("departmentDocuments", deptDocuments);
            data.put("departmentActiveDocuments", deptActiveDocuments);
            
            // Department-specific user metrics
            long deptUsers = userRepository.countByDepartment(department);
            long deptActiveUsers = userRepository.countByDepartmentAndIsActiveTrue(department);
            
            data.put("departmentUsers", deptUsers);
            data.put("departmentActiveUsers", deptActiveUsers);
            
            // Department-specific expiry metrics
            long deptActiveTracking = expiryTrackingRepository.countByDepartmentAndStatus(department, ExpiryStatus.ACTIVE);
            long deptExpiredDocuments = expiryTrackingRepository.countByDepartmentAndStatus(department, ExpiryStatus.EXPIRED);
            
            data.put("departmentActiveTracking", deptActiveTracking);
            data.put("departmentExpiredDocuments", deptExpiredDocuments);
            
            // Recent activity
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            long recentDeptActivity = auditLogRepository.countByCreatedAtAfterAndDescriptionContaining(sevenDaysAgo, department);
            data.put("recentDepartmentActivity", recentDeptActivity);
            
        } catch (Exception e) {
            logger.error("Failed to get department dashboard data for {}: {}", department, e.getMessage());
        }
        
        return data;
    }
    
    /**
     * Get user dashboard data
     */
    public Map<String, Object> getUserDashboardData(User user) {
        Map<String, Object> data = new HashMap<>();
        
        try {
            // User-specific document metrics
            long userDocuments = documentRepository.countByUploadedBy(user);
            long userActiveDocuments = documentRepository.countByUploadedByAndIsActiveTrue(user);
            
            data.put("userDocuments", userDocuments);
            data.put("userActiveDocuments", userActiveDocuments);
            
            // User-specific expiry tracking
            long userActiveTracking = expiryTrackingRepository.countByAssignedToAndStatus(user, ExpiryStatus.ACTIVE);
            long userExpiredTracking = expiryTrackingRepository.countByAssignedToAndStatus(user, ExpiryStatus.EXPIRED);
            
            data.put("userActiveTracking", userActiveTracking);
            data.put("userExpiredTracking", userExpiredTracking);
            
            // User notifications
            long unreadNotifications = notificationRepository.countByUserAndReadAtIsNull(user);
            data.put("unreadNotifications", unreadNotifications);
            
            // Recent activity
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            long recentActivity = auditLogRepository.countByCreatedAtAfterAndUsername(sevenDaysAgo, user.getUsername());
            data.put("recentActivity", recentActivity);
            
        } catch (Exception e) {
            logger.error("Failed to get user dashboard data for {}: {}", user.getUsername(), e.getMessage());
        }
        
        return data;
    }
    
    /**
     * Get system dashboard data
     */
    public Map<String, Object> getSystemDashboardData() {
        Map<String, Object> data = new HashMap<>();
        
        try {
            // System performance metrics
            data.put("systemUptime", 24 * 30); // 30 days placeholder
            data.put("averageResponseTime", 150); // ms placeholder
            data.put("errorRate", 0.01); // 1% placeholder
            data.put("cpuUsage", 45.5); // % placeholder
            data.put("memoryUsage", 67.2); // % placeholder
            data.put("diskUsage", 23.8); // % placeholder
            
            // Storage metrics
            long totalDocuments = documentRepository.count();
            double estimatedStorage = totalDocuments * 0.5; // 0.5 MB per document average
            data.put("estimatedStorageMB", estimatedStorage);
            data.put("storageUtilization", 23.8); // % placeholder
            
            // Database metrics
            data.put("databaseConnections", 15); // placeholder
            data.put("databaseResponseTime", 25); // ms placeholder
            data.put("databaseSizeMB", 1024); // MB placeholder
            
            // Cache metrics
            data.put("cacheHitRate", 0.95); // 95% placeholder
            data.put("cacheSizeMB", 256); // MB placeholder
            
        } catch (Exception e) {
            logger.error("Failed to get system dashboard data: {}", e.getMessage());
        }
        
        return data;
    }
    
    /**
     * Get compliance dashboard data
     */
    public Map<String, Object> getComplianceDashboardData() {
        Map<String, Object> data = new HashMap<>();
        
        try {
            // Compliance metrics
            long totalDocuments = documentRepository.count();
            long activeDocuments = documentRepository.countByIsActiveTrue();
            double complianceRate = (double) activeDocuments / totalDocuments * 100;
            
            data.put("totalDocuments", totalDocuments);
            data.put("activeDocuments", activeDocuments);
            data.put("complianceRate", complianceRate);
            
            // Expiry compliance
            long activeTracking = expiryTrackingRepository.countByStatus(ExpiryStatus.ACTIVE);
            long expiredDocuments = expiryTrackingRepository.countByStatus(ExpiryStatus.EXPIRED);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime sevenDaysFromNow = now.plusDays(7);
            List<ExpiryTracking> expiringIn7Days = expiryTrackingRepository.findExpiringBetween(now, sevenDaysFromNow);
            
            data.put("activeTracking", activeTracking);
            data.put("expiredDocuments", expiredDocuments);
            data.put("expiringIn7Days", expiringIn7Days.size());
            
            // Audit compliance
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            long auditLogs = auditLogRepository.countByCreatedAtAfter(thirtyDaysAgo);
            data.put("recentAuditLogs", auditLogs);
            
            // User compliance
            long totalUsers = userRepository.count();
            long activeUsers = userRepository.countByIsActiveTrue();
            double userComplianceRate = (double) activeUsers / totalUsers * 100;
            
            data.put("totalUsers", totalUsers);
            data.put("activeUsers", activeUsers);
            data.put("userComplianceRate", userComplianceRate);
            
        } catch (Exception e) {
            logger.error("Failed to get compliance dashboard data: {}", e.getMessage());
        }
        
        return data;
    }
    
    /**
     * Get analytics data for charts
     */
    public Map<String, Object> getAnalyticsData(String metricType, String dimensionKey, int days) {
        Map<String, Object> data = new HashMap<>();
        
        try {
            LocalDateTime startDate = LocalDateTime.now().minusDays(days);
            
            // Get analytics data from database
            List<Analytics> analytics = analyticsRepository.findByPeriodBetween(startDate, LocalDateTime.now());
            
            // Process data for charts
            Map<String, List<Double>> timeSeriesData = new HashMap<>();
            Map<String, Double> aggregatedData = new HashMap<>();
            
            for (Analytics analytic : analytics) {
                if (analytic.getMetricType().name().equals(metricType)) {
                    String key = analytic.getDimensionValue() != null ? analytic.getDimensionValue() : "Total";
                    
                    if (!timeSeriesData.containsKey(key)) {
                        timeSeriesData.put(key, new ArrayList<>());
                    }
                    timeSeriesData.get(key).add(analytic.getMetricValue());
                    
                    aggregatedData.put(key, aggregatedData.getOrDefault(key, 0.0) + analytic.getMetricValue());
                }
            }
            
            data.put("timeSeriesData", timeSeriesData);
            data.put("aggregatedData", aggregatedData);
            data.put("total", aggregatedData.values().stream().mapToDouble(Double::doubleValue).sum());
            
        } catch (Exception e) {
            logger.error("Failed to get analytics data: {}", e.getMessage());
        }
        
        return data;
    }
    
    /**
     * Record dashboard access
     */
    public void recordDashboardAccess(Long dashboardId) {
        try {
            Optional<Dashboard> dashboardOpt = dashboardRepository.findById(dashboardId);
            if (dashboardOpt.isPresent()) {
                Dashboard dashboard = dashboardOpt.get();
                dashboard.setAccessCount(dashboard.getAccessCount() + 1);
                dashboard.setLastAccessedAt(LocalDateTime.now());
                dashboardRepository.save(dashboard);
            }
        } catch (Exception e) {
            logger.error("Failed to record dashboard access: {}", e.getMessage());
        }
    }
    
    /**
     * Get dashboard widgets configuration
     */
    public Map<String, Object> getDashboardWidgets(DashboardType type) {
        Map<String, Object> widgets = new HashMap<>();
        
        switch (type) {
            case EXECUTIVE:
                widgets.put("widgets", Arrays.asList(
                    "document-summary-card",
                    "user-activity-card", 
                    "expiry-alerts-card",
                    "system-performance-chart",
                    "recent-activity-table"
                ));
                break;
                
            case DEPARTMENT:
                widgets.put("widgets", Arrays.asList(
                    "department-documents-card",
                    "department-users-card",
                    "department-expiry-card",
                    "department-activity-chart"
                ));
                break;
                
            case USER:
                widgets.put("widgets", Arrays.asList(
                    "user-documents-card",
                    "user-notifications-card",
                    "user-expiry-card",
                    "user-activity-chart"
                ));
                break;
                
            case SYSTEM:
                widgets.put("widgets", Arrays.asList(
                    "system-performance-card",
                    "storage-usage-card",
                    "database-metrics-card",
                    "system-health-chart"
                ));
                break;
                
            case COMPLIANCE:
                widgets.put("widgets", Arrays.asList(
                    "compliance-rate-card",
                    "expiry-compliance-card",
                    "audit-compliance-card",
                    "compliance-trends-chart"
                ));
                break;
                
            default:
                widgets.put("widgets", Arrays.asList("default-card"));
                break;
        }
        
        return widgets;
    }
    
    /**
     * Generate dashboard report
     */
    public String generateDashboardReport(DashboardType type, ReportFormat format) {
        try {
            Map<String, Object> data;
            
            switch (type) {
                case EXECUTIVE:
                    data = getExecutiveDashboardData();
                    break;
                case DEPARTMENT:
                    data = getDepartmentDashboardData("All");
                    break;
                case USER:
                    data = getUserDashboardData(null); // Will need user context
                    break;
                case SYSTEM:
                    data = getSystemDashboardData();
                    break;
                case COMPLIANCE:
                    data = getComplianceDashboardData();
                    break;
                default:
                    data = new HashMap<>();
                    break;
            }
            
            // Generate report based on format
            switch (format) {
                case PDF:
                    return reportingService.pdfReportService.generateExecutiveSummary(data);
                case EXCEL:
                    return reportingService.excelReportService.generateDepartmentSummary(data);
                case WORD:
                    return reportingService.wordReportService.generateComplianceReport(data);
                default:
                    return null;
            }
            
        } catch (Exception e) {
            logger.error("Failed to generate dashboard report: {}", e.getMessage());
            return null;
        }
    }
}
