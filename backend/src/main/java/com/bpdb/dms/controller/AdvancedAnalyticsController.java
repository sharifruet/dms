package com.bpdb.dms.controller;

import com.bpdb.dms.entity.Analytics;
import com.bpdb.dms.entity.MetricType;
import com.bpdb.dms.service.AdvancedAnalyticsServiceSimple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Advanced Analytics
 */
@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AdvancedAnalyticsController {
    
    @Autowired
    private AdvancedAnalyticsServiceSimple analyticsService;
    
    /**
     * Create analytics data
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<Analytics> createAnalyticsData(
            @RequestParam MetricType metricType,
            @RequestParam String metricName,
            @RequestParam Double metricValue,
            @RequestParam(required = false) String dimensionKey,
            @RequestParam(required = false) String dimensionValue) {
        
        try {
            Analytics created = analyticsService.createAnalyticsData(
                metricType, metricName, metricValue, dimensionKey, dimensionValue, LocalDateTime.now());
            
            return ResponseEntity.ok(created);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get analytics data
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Page<Analytics>> getAnalyticsData(
            @RequestParam(required = false) MetricType metricType,
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Analytics> data = analyticsService.getAnalyticsData(
            metricType, metricName, startDate, endDate, pageable);
        
        return ResponseEntity.ok(data);
    }
    
    /**
     * Get analytics insights
     */
    @GetMapping("/insights")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getAnalyticsInsights(
            @RequestParam(required = false) MetricType metricType,
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {
        
        Map<String, Object> insights = analyticsService.getAnalyticsInsights(
            metricType, metricName, startDate, endDate);
        
        return ResponseEntity.ok(insights);
    }
    
    /**
     * Get analytics trends
     */
    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<List<Map<String, Object>>> getAnalyticsTrends(
            @RequestParam String metricName,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "day") String granularity) {
        
        List<Map<String, Object>> trends = analyticsService.getAnalyticsTrends(
            metricName, startDate, endDate, granularity);
        
        return ResponseEntity.ok(trends);
    }
    
    /**
     * Get analytics aggregations
     */
    @GetMapping("/aggregations")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getAnalyticsAggregations(
            @RequestParam String metricName,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "sum") String aggregationType) {
        
        Map<String, Object> aggregations = analyticsService.getAnalyticsAggregations(
            metricName, startDate, endDate, aggregationType);
        
        return ResponseEntity.ok(aggregations);
    }
    
    /**
     * Get analytics comparisons
     */
    @GetMapping("/comparisons")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getAnalyticsComparisons(
            @RequestParam String metricName,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate,
            @RequestParam String comparisonPeriod) {
        
        Map<String, Object> comparisons = analyticsService.getAnalyticsComparisons(
            metricName, startDate, endDate, comparisonPeriod);
        
        return ResponseEntity.ok(comparisons);
    }
    
    /**
     * Get analytics statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getAnalyticsStatistics() {
        Map<String, Object> statistics = analyticsService.getAnalyticsStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * Export analytics data
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<byte[]> exportAnalyticsData(
            @RequestParam(required = false) MetricType metricType,
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "csv") String format) {
        
        try {
            byte[] data = analyticsService.exportAnalyticsData(
                metricType, metricName, startDate, endDate, format);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "analytics_export.csv");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(data);
                
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

