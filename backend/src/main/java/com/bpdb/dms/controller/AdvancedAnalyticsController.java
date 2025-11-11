package com.bpdb.dms.controller;

import com.bpdb.dms.entity.AnalyticsData;
import com.bpdb.dms.entity.AnalyticsType;
import com.bpdb.dms.service.AdvancedAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AdvancedAnalyticsController {

    @Autowired
    private AdvancedAnalyticsService analyticsService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<AnalyticsData> createAnalyticsData(@RequestBody AnalyticsData analyticsData) {
        try {
            AnalyticsData created = analyticsService.createAnalyticsData(
                analyticsData.getAnalyticsType(),
                analyticsData.getMetricName(),
                analyticsData.getMetricValue(),
                Map.of(),
                analyticsData.getSourceSystem(),
                analyticsData.getTimestamp()
            );
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Page<AnalyticsData>> getAnalyticsData(
            @RequestParam(required = false) AnalyticsType analyticsType,
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            Pageable pageable) {
        Page<AnalyticsData> data = analyticsService.getAnalyticsData(
            analyticsType, metricName, startDate, endDate, pageable);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/insights")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getAnalyticsInsights(
            @RequestParam(required = false) AnalyticsType analyticsType,
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {
        Map<String, Object> insights = analyticsService.getAnalyticsInsights(
            analyticsType, metricName, startDate, endDate);
        return ResponseEntity.ok(insights);
    }

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

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getAnalyticsStatistics() {
        Map<String, Object> statistics = analyticsService.getAnalyticsStatistics();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<Map<String, Object>> exportAnalyticsData(
            @RequestParam(required = false) AnalyticsType analyticsType,
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "csv") String format) {
        Map<String, Object> exportResult = analyticsService.exportAnalyticsData(
            analyticsType, metricName, startDate, endDate, format);
        return ResponseEntity.ok(exportResult);
    }
}

