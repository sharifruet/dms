package com.bpdb.dms.controller;

import com.bpdb.dms.entity.*;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Controller for dashboard management
 */
@RestController
@RequestMapping("/api/dashboards")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UserRepository userRepository;
    
    /**
     * Create a new dashboard
     */
    @PostMapping
    public ResponseEntity<Dashboard> createDashboard(
            @RequestBody CreateDashboardRequest request,
            Authentication authentication) {
        
        try {
            User user = resolveCurrentUser(authentication);
            
            Dashboard dashboard = dashboardService.createDashboard(
                request.getName(),
                request.getDescription(),
                request.getType(),
                user,
                request.getLayoutConfig(),
                request.getWidgetsConfig()
            );
            
            return ResponseEntity.ok(dashboard);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get dashboards
     */
    @GetMapping
    public ResponseEntity<Page<Dashboard>> getDashboards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        try {
            User user = resolveCurrentUser(authentication);
            Pageable pageable = PageRequest.of(page, size);
            Page<Dashboard> dashboards = dashboardService.dashboardRepository.findAccessibleToUser(user, pageable);
            
            return ResponseEntity.ok(dashboards);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get dashboard by ID
     */
    @GetMapping("/{dashboardId}")
    public ResponseEntity<Dashboard> getDashboard(@PathVariable Long dashboardId) {
        try {
            Optional<Dashboard> dashboard = dashboardService.dashboardRepository.findById(dashboardId);
            
            if (dashboard.isPresent()) {
                // Record access
                dashboardService.recordDashboardAccess(dashboardId);
                return ResponseEntity.ok(dashboard.get());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get executive dashboard data
     */
    @GetMapping("/data/executive")
    public ResponseEntity<Map<String, Object>> getExecutiveDashboardData() {
        try {
            Map<String, Object> data = dashboardService.getExecutiveDashboardData();
            return ResponseEntity.ok(data);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get department dashboard data
     */
    @GetMapping("/data/department")
    public ResponseEntity<Map<String, Object>> getDepartmentDashboardData(
            @RequestParam String department) {
        
        try {
            Map<String, Object> data = dashboardService.getDepartmentDashboardData(department);
            return ResponseEntity.ok(data);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get user dashboard data
     */
    @GetMapping("/data/user")
    public ResponseEntity<Map<String, Object>> getUserDashboardData(Authentication authentication) {
        try {
            User user = resolveCurrentUser(authentication);
            Map<String, Object> data = dashboardService.getUserDashboardData(user);
            return ResponseEntity.ok(data);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private User resolveCurrentUser(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("Missing authentication context");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + authentication.getName()));
    }
    
    /**
     * Get system dashboard data
     */
    @GetMapping("/data/system")
    public ResponseEntity<Map<String, Object>> getSystemDashboardData() {
        try {
            Map<String, Object> data = dashboardService.getSystemDashboardData();
            return ResponseEntity.ok(data);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get compliance dashboard data
     */
    @GetMapping("/data/compliance")
    public ResponseEntity<Map<String, Object>> getComplianceDashboardData() {
        try {
            Map<String, Object> data = dashboardService.getComplianceDashboardData();
            return ResponseEntity.ok(data);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get analytics data for charts
     */
    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalyticsData(
            @RequestParam String metricType,
            @RequestParam String dimensionKey,
            @RequestParam(defaultValue = "30") int days) {
        
        try {
            Map<String, Object> data = dashboardService.getAnalyticsData(metricType, dimensionKey, days);
            return ResponseEntity.ok(data);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get dashboard widgets configuration
     */
    @GetMapping("/widgets/{type}")
    public ResponseEntity<Map<String, Object>> getDashboardWidgets(@PathVariable String type) {
        try {
            DashboardType dashboardType = DashboardType.valueOf(type.toUpperCase());
            Map<String, Object> widgets = dashboardService.getDashboardWidgets(dashboardType);
            return ResponseEntity.ok(widgets);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Generate dashboard report
     */
    @PostMapping("/{dashboardId}/report")
    public ResponseEntity<Map<String, String>> generateDashboardReport(
            @PathVariable Long dashboardId,
            @RequestParam String format) {
        
        try {
            Optional<Dashboard> dashboardOpt = dashboardService.dashboardRepository.findById(dashboardId);
            
            if (dashboardOpt.isPresent()) {
                Dashboard dashboard = dashboardOpt.get();
                ReportFormat reportFormat = ReportFormat.valueOf(format.toUpperCase());
                
                String reportPath = dashboardService.generateDashboardReport(dashboard.getType(), reportFormat);
                
                if (reportPath != null) {
                    return ResponseEntity.ok(Map.of("reportPath", reportPath, "message", "Report generated successfully"));
                } else {
                    return ResponseEntity.internalServerError()
                        .body(Map.of("error", "Failed to generate report"));
                }
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to generate dashboard report"));
        }
    }
    
    /**
     * Get default dashboard
     */
    @GetMapping("/default/{type}")
    public ResponseEntity<Dashboard> getDefaultDashboard(@PathVariable String type) {
        try {
            DashboardType dashboardType = DashboardType.valueOf(type.toUpperCase());
            Dashboard dashboard = dashboardService.dashboardRepository.findByTypeAndIsDefaultTrue(dashboardType);
            
            if (dashboard != null) {
                return ResponseEntity.ok(dashboard);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Create dashboard request DTO
     */
    public static class CreateDashboardRequest {
        private String name;
        private String description;
        private DashboardType type;
        private String layoutConfig;
        private String widgetsConfig;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public DashboardType getType() { return type; }
        public void setType(DashboardType type) { this.type = type; }
        public String getLayoutConfig() { return layoutConfig; }
        public void setLayoutConfig(String layoutConfig) { this.layoutConfig = layoutConfig; }
        public String getWidgetsConfig() { return widgetsConfig; }
        public void setWidgetsConfig(String widgetsConfig) { this.widgetsConfig = widgetsConfig; }
    }
}
