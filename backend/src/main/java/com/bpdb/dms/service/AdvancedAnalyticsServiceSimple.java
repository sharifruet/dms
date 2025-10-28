package com.bpdb.dms.service;

import com.bpdb.dms.entity.Analytics;
import com.bpdb.dms.entity.MetricType;
import com.bpdb.dms.repository.AnalyticsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Simplified Advanced Analytics Service
 */
@Service
@Transactional
public class AdvancedAnalyticsServiceSimple {
    
    @Autowired
    private AnalyticsRepository analyticsRepository;
    
    /**
     * Create analytics data
     */
    public Analytics createAnalyticsData(
            MetricType metricType,
            String metricName,
            Double metricValue,
            String dimensionKey,
            String dimensionValue,
            LocalDateTime timestamp) {
        
        Analytics analytics = new Analytics();
        analytics.setMetricType(metricType);
        analytics.setMetricName(metricName);
        analytics.setMetricValue(metricValue);
        analytics.setDimensionKey(dimensionKey);
        analytics.setDimensionValue(dimensionValue);
        analytics.setCreatedAt(timestamp != null ? timestamp : LocalDateTime.now());
        
        return analyticsRepository.save(analytics);
    }
    
    /**
     * Get analytics data with filtering
     */
    public Page<Analytics> getAnalyticsData(
            MetricType metricType,
            String metricName,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {
        
        if (metricType != null && metricName != null && startDate != null && endDate != null) {
            return analyticsRepository.findByMetricTypeAndMetricNameAndCreatedAtBetween(
                metricType, metricName, startDate, endDate, pageable);
        } else if (metricType != null && startDate != null && endDate != null) {
            return analyticsRepository.findByMetricTypeAndCreatedAtBetween(
                metricType, startDate, endDate, pageable);
        } else if (metricName != null && startDate != null && endDate != null) {
            return analyticsRepository.findByMetricNameAndCreatedAtBetween(
                metricName, startDate, endDate, pageable);
        } else if (metricType != null) {
            return analyticsRepository.findByMetricType(metricType, pageable);
        } else {
            return analyticsRepository.findAll(pageable);
        }
    }
    
    /**
     * Get analytics insights
     */
    public Map<String, Object> getAnalyticsInsights(
            MetricType metricType,
            String metricName,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        
        Map<String, Object> insights = new HashMap<>();
        
        List<Analytics> data;
        if (metricType != null && startDate != null && endDate != null) {
            data = analyticsRepository.findByMetricTypeAndCreatedAtBetween(
                metricType, startDate, endDate);
        } else {
            data = analyticsRepository.findAll();
        }
        
        if (!data.isEmpty()) {
            DoubleSummaryStatistics stats = data.stream()
                .mapToDouble(Analytics::getMetricValue)
                .summaryStatistics();
            
            insights.put("count", stats.getCount());
            insights.put("average", stats.getAverage());
            insights.put("min", stats.getMin());
            insights.put("max", stats.getMax());
            insights.put("sum", stats.getSum());
        } else {
            insights.put("count", 0);
            insights.put("average", 0.0);
            insights.put("min", 0.0);
            insights.put("max", 0.0);
            insights.put("sum", 0.0);
        }
        
        return insights;
    }
    
    /**
     * Get analytics trends
     */
    public List<Map<String, Object>> getAnalyticsTrends(
            String metricName,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String granularity) {
        
        List<Map<String, Object>> trends = new ArrayList<>();
        
        List<Analytics> data;
        if (startDate != null && endDate != null) {
            data = analyticsRepository.findByMetricNameAndCreatedAtBetween(
                metricName, startDate, endDate);
        } else {
            data = analyticsRepository.findByMetricName(metricName);
        }
        
        // Group by date and calculate aggregates
        Map<String, List<Analytics>> groupedData = new HashMap<>();
        for (Analytics analytics : data) {
            String key = analytics.getCreatedAt().toLocalDate().toString();
            groupedData.computeIfAbsent(key, k -> new ArrayList<>()).add(analytics);
        }
        
        for (Map.Entry<String, List<Analytics>> entry : groupedData.entrySet()) {
            double avg = entry.getValue().stream()
                .mapToDouble(Analytics::getMetricValue)
                .average()
                .orElse(0.0);
            
            Map<String, Object> trend = new HashMap<>();
            trend.put("date", entry.getKey());
            trend.put("value", avg);
            trend.put("count", entry.getValue().size());
            trends.add(trend);
        }
        
        return trends;
    }
    
    /**
     * Get analytics aggregations
     */
    public Map<String, Object> getAnalyticsAggregations(
            String metricName,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String aggregationType) {
        
        Map<String, Object> aggregations = new HashMap<>();
        
        List<Analytics> data;
        if (startDate != null && endDate != null) {
            data = analyticsRepository.findByMetricNameAndCreatedAtBetween(
                metricName, startDate, endDate);
        } else {
            data = analyticsRepository.findByMetricName(metricName);
        }
        
        if (!data.isEmpty()) {
            double value = 0.0;
            switch (aggregationType.toLowerCase()) {
                case "sum":
                    value = data.stream().mapToDouble(Analytics::getMetricValue).sum();
                    break;
                case "avg":
                case "average":
                    value = data.stream().mapToDouble(Analytics::getMetricValue).average().orElse(0.0);
                    break;
                case "min":
                    value = data.stream().mapToDouble(Analytics::getMetricValue).min().orElse(0.0);
                    break;
                case "max":
                    value = data.stream().mapToDouble(Analytics::getMetricValue).max().orElse(0.0);
                    break;
                case "count":
                    value = data.size();
                    break;
            }
            aggregations.put("value", value);
            aggregations.put("type", aggregationType);
            aggregations.put("count", data.size());
        }
        
        return aggregations;
    }
    
    /**
     * Get analytics comparisons
     */
    public Map<String, Object> getAnalyticsComparisons(
            String metricName,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String comparisonPeriod) {
        
        Map<String, Object> comparisons = new HashMap<>();
        
        List<Analytics> currentData = analyticsRepository.findByMetricNameAndCreatedAtBetween(
            metricName, startDate, endDate);
        
        // Calculate previous period dates
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        LocalDateTime prevEndDate = startDate.minusDays(1);
        LocalDateTime prevStartDate = prevEndDate.minusDays(daysDiff);
        
        List<Analytics> previousData = analyticsRepository.findByMetricNameAndCreatedAtBetween(
            metricName, prevStartDate, prevEndDate);
        
        double currentAvg = currentData.stream()
            .mapToDouble(Analytics::getMetricValue)
            .average()
            .orElse(0.0);
        
        double previousAvg = previousData.stream()
            .mapToDouble(Analytics::getMetricValue)
            .average()
            .orElse(0.0);
        
        double change = previousAvg != 0 ? ((currentAvg - previousAvg) / previousAvg) * 100 : 0.0;
        
        comparisons.put("currentPeriod", Map.of("average", currentAvg, "count", currentData.size()));
        comparisons.put("previousPeriod", Map.of("average", previousAvg, "count", previousData.size()));
        comparisons.put("percentageChange", change);
        
        return comparisons;
    }
    
    /**
     * Get analytics statistics
     */
    public Map<String, Object> getAnalyticsStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        long totalRecords = analyticsRepository.count();
        statistics.put("totalRecords", totalRecords);
        
        // Get counts by metric type
        for (MetricType type : MetricType.values()) {
            long count = analyticsRepository.countByMetricType(type);
            statistics.put(type.name() + "_count", count);
        }
        
        return statistics;
    }
    
    /**
     * Export analytics data
     */
    public byte[] exportAnalyticsData(
            MetricType metricType,
            String metricName,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String format) {
        
        List<Analytics> data;
        if (metricType != null && startDate != null && endDate != null) {
            data = analyticsRepository.findByMetricTypeAndCreatedAtBetween(
                metricType, startDate, endDate);
        } else {
            data = analyticsRepository.findAll();
        }
        
        // Simple CSV export
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Metric Type,Metric Name,Value,Dimension Key,Dimension Value,Created At\n");
        
        for (Analytics analytics : data) {
            csv.append(analytics.getId()).append(",")
                .append(analytics.getMetricType()).append(",")
                .append(analytics.getMetricName()).append(",")
                .append(analytics.getMetricValue()).append(",")
                .append(analytics.getDimensionKey() != null ? analytics.getDimensionKey() : "").append(",")
                .append(analytics.getDimensionValue() != null ? analytics.getDimensionValue() : "").append(",")
                .append(analytics.getCreatedAt()).append("\n");
        }
        
        return csv.toString().getBytes();
    }
}

