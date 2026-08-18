package com.bpdb.dms.controller;

import com.bpdb.dms.entity.*;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.ReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for reporting and analytics
 */
@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportingController {
    
    @Autowired
    private ReportingService reportingService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Helper method to get User from Authentication
     */
    private User getUserFromAuthentication(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    /**
     * Create a new report
     */
    @PostMapping
    public ResponseEntity<Report> createReport(
            @RequestBody CreateReportRequest request,
            Authentication authentication) {
        
        try {
            User user = getUserFromAuthentication(authentication);
            
            Report report = reportingService.createReport(
                request.getName(),
                request.getDescription(),
                request.getType(),
                request.getFormat(),
                user,
                request.getParameters()
            );
            
            return ResponseEntity.ok(report);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get reports
     */
    @GetMapping
    public ResponseEntity<Page<Report>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        try {
            User user = getUserFromAuthentication(authentication);
            Pageable pageable = PageRequest.of(page, size);
            Page<Report> reports = reportingService.reportRepository.findByCreatedBy(user, pageable);
            
            return ResponseEntity.ok(reports);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get public reports
     */
    @GetMapping("/public")
    public ResponseEntity<Page<Report>> getPublicReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Report> reports = reportingService.reportRepository.findByIsPublicTrue(pageable);
            
            return ResponseEntity.ok(reports);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get report by ID
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<Report> getReport(@PathVariable Long reportId) {
        try {
            Optional<Report> report = reportingService.reportRepository.findById(reportId);
            
            if (report.isPresent()) {
                return ResponseEntity.ok(report.get());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get document summary data
     */
    @GetMapping("/data/document-summary")
    public ResponseEntity<Map<String, Object>> getDocumentSummaryData(
            @RequestParam Map<String, Object> parameters) {
        
        try {
            Map<String, Object> data = reportingService.getDocumentSummaryData(parameters);
            return ResponseEntity.ok(data);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get user activity data
     */
    @GetMapping("/data/user-activity")
    public ResponseEntity<Map<String, Object>> getUserActivityData(
            @RequestParam Map<String, Object> parameters) {
        
        try {
            Map<String, Object> data = reportingService.getUserActivityData(parameters);
            return ResponseEntity.ok(data);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get expiry report data
     */
    @GetMapping("/data/expiry-report")
    public ResponseEntity<Map<String, Object>> getExpiryReportData(
            @RequestParam Map<String, Object> parameters) {
        
        try {
            Map<String, Object> data = reportingService.getExpiryReportData(parameters);
            return ResponseEntity.ok(data);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get system performance data
     */
    @GetMapping("/data/system-performance")
    public ResponseEntity<Map<String, Object>> getSystemPerformanceData(
            @RequestParam Map<String, Object> parameters) {
        
        try {
            Map<String, Object> data = reportingService.getSystemPerformanceData(parameters);
            return ResponseEntity.ok(data);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Create report request DTO
     */
    public static class CreateReportRequest {
        private String name;
        private String description;
        private ReportType type;
        private ReportFormat format;
        private Map<String, Object> parameters;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public ReportType getType() { return type; }
        public void setType(ReportType type) { this.type = type; }
        public ReportFormat getFormat() { return format; }
        public void setFormat(ReportFormat format) { this.format = format; }
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    }
    
}
