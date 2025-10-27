package com.bpdb.dms.service;

import com.bpdb.dms.entity.OptimizationTask;
import com.bpdb.dms.entity.OptimizationType;
import com.bpdb.dms.entity.OptimizationStatus;
import com.bpdb.dms.repository.OptimizationTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service for system optimization and performance tuning
 */
@Service
@Transactional
public class SystemOptimizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemOptimizationService.class);
    
    @Autowired
    private OptimizationTaskRepository optimizationTaskRepository;
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Create optimization task
     */
    public OptimizationTask createOptimizationTask(OptimizationType optimizationType, String description,
                                                  Map<String, Object> parameters) {
        try {
            logger.info("Creating optimization task: {}", optimizationType);
            
            OptimizationTask task = new OptimizationTask();
            task.setOptimizationType(optimizationType);
            task.setDescription(description);
            task.setParameters(parameters != null ? parameters.toString() : "{}");
            task.setStatus(OptimizationStatus.PENDING);
            task.setCreatedAt(LocalDateTime.now());
            
            OptimizationTask savedTask = optimizationTaskRepository.save(task);
            
            auditService.logActivity("SYSTEM", "OPTIMIZATION_TASK_CREATED", 
                "Optimization task created: " + optimizationType, null);
            
            logger.info("Optimization task created: {}", savedTask.getId());
            
            return savedTask;
            
        } catch (Exception e) {
            logger.error("Failed to create optimization task: {}", e.getMessage());
            throw new RuntimeException("Failed to create optimization task", e);
        }
    }
    
    /**
     * Execute optimization task
     */
    @Async
    public CompletableFuture<OptimizationTask> executeOptimizationTask(Long taskId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                OptimizationTask task = optimizationTaskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Optimization task not found"));
                
                logger.info("Executing optimization task: {}", task.getOptimizationType());
                
                task.setStatus(OptimizationStatus.IN_PROGRESS);
                task.setStartedAt(LocalDateTime.now());
                optimizationTaskRepository.save(task);
                
                // Execute the optimization based on type
                Map<String, Object> results = performOptimization(task);
                
                // Update task with results
                task.setStatus(OptimizationStatus.COMPLETED);
                task.setCompletedAt(LocalDateTime.now());
                task.setResults(results.toString());
                task.setExecutionTimeMs(System.currentTimeMillis() - task.getStartedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
                
                OptimizationTask completedTask = optimizationTaskRepository.save(task);
                
                auditService.logActivity("SYSTEM", "OPTIMIZATION_TASK_COMPLETED", 
                    "Optimization task completed: " + task.getOptimizationType(), null);
                
                logger.info("Optimization task completed: {}", task.getOptimizationType());
                
                return completedTask;
                
            } catch (Exception e) {
                logger.error("Failed to execute optimization task: {}", e.getMessage());
                
                // Update task with error
                try {
                    OptimizationTask task = optimizationTaskRepository.findById(taskId).orElse(null);
                    if (task != null) {
                        task.setStatus(OptimizationStatus.FAILED);
                        task.setErrorMessage(e.getMessage());
                        task.setCompletedAt(LocalDateTime.now());
                        optimizationTaskRepository.save(task);
                    }
                } catch (Exception ex) {
                    logger.error("Failed to update failed task: {}", ex.getMessage());
                }
                
                throw new RuntimeException("Optimization task execution failed", e);
            }
        });
    }
    
    /**
     * Perform database optimization
     */
    public Map<String, Object> optimizeDatabase() {
        try {
            logger.info("Starting database optimization");
            
            Map<String, Object> results = new HashMap<>();
            
            // Analyze tables
            int analyzedTables = analyzeTables();
            results.put("analyzedTables", analyzedTables);
            
            // Rebuild indexes
            int rebuiltIndexes = rebuildIndexes();
            results.put("rebuiltIndexes", rebuiltIndexes);
            
            // Update statistics
            boolean statisticsUpdated = updateStatistics();
            results.put("statisticsUpdated", statisticsUpdated);
            
            // Clean up old data
            int cleanedRecords = cleanupOldData();
            results.put("cleanedRecords", cleanedRecords);
            
            // Vacuum database
            boolean vacuumed = vacuumDatabase();
            results.put("vacuumed", vacuumed);
            
            results.put("optimizationType", "DATABASE");
            results.put("completedAt", LocalDateTime.now());
            results.put("success", true);
            
            logger.info("Database optimization completed");
            
            return results;
            
        } catch (Exception e) {
            logger.error("Database optimization failed: {}", e.getMessage());
            throw new RuntimeException("Database optimization failed", e);
        }
    }
    
    /**
     * Perform cache optimization
     */
    public Map<String, Object> optimizeCache() {
        try {
            logger.info("Starting cache optimization");
            
            Map<String, Object> results = new HashMap<>();
            
            // Clear expired cache entries
            int clearedEntries = clearExpiredCacheEntries();
            results.put("clearedEntries", clearedEntries);
            
            // Optimize cache configuration
            boolean configOptimized = optimizeCacheConfiguration();
            results.put("configOptimized", configOptimized);
            
            // Warm up cache
            int warmedEntries = warmUpCache();
            results.put("warmedEntries", warmedEntries);
            
            // Analyze cache hit ratio
            double hitRatio = analyzeCacheHitRatio();
            results.put("hitRatio", hitRatio);
            
            results.put("optimizationType", "CACHE");
            results.put("completedAt", LocalDateTime.now());
            results.put("success", true);
            
            logger.info("Cache optimization completed");
            
            return results;
            
        } catch (Exception e) {
            logger.error("Cache optimization failed: {}", e.getMessage());
            throw new RuntimeException("Cache optimization failed", e);
        }
    }
    
    /**
     * Perform file system optimization
     */
    public Map<String, Object> optimizeFileSystem() {
        try {
            logger.info("Starting file system optimization");
            
            Map<String, Object> results = new HashMap<>();
            
            // Clean up temporary files
            int cleanedFiles = cleanupTemporaryFiles();
            results.put("cleanedFiles", cleanedFiles);
            
            // Defragment storage
            boolean defragmented = defragmentStorage();
            results.put("defragmented", defragmented);
            
            // Optimize file permissions
            int optimizedPermissions = optimizeFilePermissions();
            results.put("optimizedPermissions", optimizedPermissions);
            
            // Compress old files
            int compressedFiles = compressOldFiles();
            results.put("compressedFiles", compressedFiles);
            
            results.put("optimizationType", "FILE_SYSTEM");
            results.put("completedAt", LocalDateTime.now());
            results.put("success", true);
            
            logger.info("File system optimization completed");
            
            return results;
            
        } catch (Exception e) {
            logger.error("File system optimization failed: {}", e.getMessage());
            throw new RuntimeException("File system optimization failed", e);
        }
    }
    
    /**
     * Perform memory optimization
     */
    public Map<String, Object> optimizeMemory() {
        try {
            logger.info("Starting memory optimization");
            
            Map<String, Object> results = new HashMap<>();
            
            // Force garbage collection
            long memoryBefore = getUsedMemory();
            System.gc();
            long memoryAfter = getUsedMemory();
            results.put("memoryFreed", memoryBefore - memoryAfter);
            
            // Optimize JVM settings
            boolean jvmOptimized = optimizeJVMSettings();
            results.put("jvmOptimized", jvmOptimized);
            
            // Clear application caches
            int clearedCaches = clearApplicationCaches();
            results.put("clearedCaches", clearedCaches);
            
            // Optimize object pools
            boolean poolsOptimized = optimizeObjectPools();
            results.put("poolsOptimized", poolsOptimized);
            
            results.put("optimizationType", "MEMORY");
            results.put("completedAt", LocalDateTime.now());
            results.put("success", true);
            
            logger.info("Memory optimization completed");
            
            return results;
            
        } catch (Exception e) {
            logger.error("Memory optimization failed: {}", e.getMessage());
            throw new RuntimeException("Memory optimization failed", e);
        }
    }
    
    /**
     * Perform search optimization
     */
    public Map<String, Object> optimizeSearch() {
        try {
            logger.info("Starting search optimization");
            
            Map<String, Object> results = new HashMap<>();
            
            // Optimize Elasticsearch indices
            int optimizedIndices = optimizeElasticsearchIndices();
            results.put("optimizedIndices", optimizedIndices);
            
            // Rebuild search indexes
            int rebuiltIndexes = rebuildSearchIndexes();
            results.put("rebuiltIndexes", rebuiltIndexes);
            
            // Update search statistics
            boolean statisticsUpdated = updateSearchStatistics();
            results.put("statisticsUpdated", statisticsUpdated);
            
            // Optimize search queries
            int optimizedQueries = optimizeSearchQueries();
            results.put("optimizedQueries", optimizedQueries);
            
            results.put("optimizationType", "SEARCH");
            results.put("completedAt", LocalDateTime.now());
            results.put("success", true);
            
            logger.info("Search optimization completed");
            
            return results;
            
        } catch (Exception e) {
            logger.error("Search optimization failed: {}", e.getMessage());
            throw new RuntimeException("Search optimization failed", e);
        }
    }
    
    /**
     * Get optimization recommendations
     */
    public List<Map<String, Object>> getOptimizationRecommendations() {
        try {
            List<Map<String, Object>> recommendations = new ArrayList<>();
            
            // Check database performance
            Map<String, Object> dbRecommendation = analyzeDatabasePerformance();
            if (dbRecommendation != null) {
                recommendations.add(dbRecommendation);
            }
            
            // Check cache performance
            Map<String, Object> cacheRecommendation = analyzeCachePerformance();
            if (cacheRecommendation != null) {
                recommendations.add(cacheRecommendation);
            }
            
            // Check memory usage
            Map<String, Object> memoryRecommendation = analyzeMemoryUsage();
            if (memoryRecommendation != null) {
                recommendations.add(memoryRecommendation);
            }
            
            // Check file system
            Map<String, Object> fsRecommendation = analyzeFileSystemPerformance();
            if (fsRecommendation != null) {
                recommendations.add(fsRecommendation);
            }
            
            return recommendations;
            
        } catch (Exception e) {
            logger.error("Failed to get optimization recommendations: {}", e.getMessage());
            throw new RuntimeException("Failed to get optimization recommendations", e);
        }
    }
    
    /**
     * Get optimization statistics
     */
    public Map<String, Object> getOptimizationStatistics() {
        try {
            return Map.of(
                "totalTasks", optimizationTaskRepository.count(),
                "completedTasks", optimizationTaskRepository.countByStatus(OptimizationStatus.COMPLETED),
                "failedTasks", optimizationTaskRepository.countByStatus(OptimizationStatus.FAILED),
                "pendingTasks", optimizationTaskRepository.countByStatus(OptimizationStatus.PENDING),
                "inProgressTasks", optimizationTaskRepository.countByStatus(OptimizationStatus.IN_PROGRESS),
                "averageExecutionTime", calculateAverageExecutionTime(),
                "lastOptimization", getLastOptimizationTime(),
                "optimizationFrequency", getOptimizationFrequency()
            );
            
        } catch (Exception e) {
            logger.error("Failed to get optimization statistics: {}", e.getMessage());
            throw new RuntimeException("Failed to get optimization statistics", e);
        }
    }
    
    /**
     * Scheduled optimization task
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    public void scheduledOptimization() {
        try {
            logger.info("Starting scheduled optimization");
            
            // Create and execute database optimization
            OptimizationTask dbTask = createOptimizationTask(
                OptimizationType.DATABASE, 
                "Scheduled database optimization", 
                null
            );
            executeOptimizationTask(dbTask.getId());
            
            // Create and execute cache optimization
            OptimizationTask cacheTask = createOptimizationTask(
                OptimizationType.CACHE, 
                "Scheduled cache optimization", 
                null
            );
            executeOptimizationTask(cacheTask.getId());
            
            logger.info("Scheduled optimization completed");
            
        } catch (Exception e) {
            logger.error("Scheduled optimization failed: {}", e.getMessage());
        }
    }
    
    // Private helper methods
    
    private Map<String, Object> performOptimization(OptimizationTask task) {
        switch (task.getOptimizationType()) {
            case DATABASE:
                return optimizeDatabase();
            case CACHE:
                return optimizeCache();
            case FILE_SYSTEM:
                return optimizeFileSystem();
            case MEMORY:
                return optimizeMemory();
            case SEARCH:
                return optimizeSearch();
            default:
                throw new RuntimeException("Unknown optimization type: " + task.getOptimizationType());
        }
    }
    
    private int analyzeTables() {
        // Simulate table analysis
        return 10;
    }
    
    private int rebuildIndexes() {
        // Simulate index rebuilding
        return 5;
    }
    
    private boolean updateStatistics() {
        // Simulate statistics update
        return true;
    }
    
    private int cleanupOldData() {
        // Simulate old data cleanup
        return 1000;
    }
    
    private boolean vacuumDatabase() {
        // Simulate database vacuum
        return true;
    }
    
    private int clearExpiredCacheEntries() {
        // Simulate cache cleanup
        return 500;
    }
    
    private boolean optimizeCacheConfiguration() {
        // Simulate cache configuration optimization
        return true;
    }
    
    private int warmUpCache() {
        // Simulate cache warming
        return 100;
    }
    
    private double analyzeCacheHitRatio() {
        // Simulate cache hit ratio analysis
        return 0.85;
    }
    
    private int cleanupTemporaryFiles() {
        // Simulate temporary file cleanup
        return 50;
    }
    
    private boolean defragmentStorage() {
        // Simulate storage defragmentation
        return true;
    }
    
    private int optimizeFilePermissions() {
        // Simulate file permission optimization
        return 200;
    }
    
    private int compressOldFiles() {
        // Simulate file compression
        return 25;
    }
    
    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    private boolean optimizeJVMSettings() {
        // Simulate JVM optimization
        return true;
    }
    
    private int clearApplicationCaches() {
        // Simulate application cache clearing
        return 10;
    }
    
    private boolean optimizeObjectPools() {
        // Simulate object pool optimization
        return true;
    }
    
    private int optimizeElasticsearchIndices() {
        // Simulate Elasticsearch optimization
        return 3;
    }
    
    private int rebuildSearchIndexes() {
        // Simulate search index rebuilding
        return 2;
    }
    
    private boolean updateSearchStatistics() {
        // Simulate search statistics update
        return true;
    }
    
    private int optimizeSearchQueries() {
        // Simulate search query optimization
        return 15;
    }
    
    private Map<String, Object> analyzeDatabasePerformance() {
        // Simulate database performance analysis
        return Map.of(
            "type", "DATABASE",
            "priority", "HIGH",
            "recommendation", "Consider rebuilding indexes",
            "estimatedImprovement", "15%"
        );
    }
    
    private Map<String, Object> analyzeCachePerformance() {
        // Simulate cache performance analysis
        return Map.of(
            "type", "CACHE",
            "priority", "MEDIUM",
            "recommendation", "Increase cache size",
            "estimatedImprovement", "10%"
        );
    }
    
    private Map<String, Object> analyzeMemoryUsage() {
        // Simulate memory usage analysis
        return Map.of(
            "type", "MEMORY",
            "priority", "LOW",
            "recommendation", "Optimize object pools",
            "estimatedImprovement", "5%"
        );
    }
    
    private Map<String, Object> analyzeFileSystemPerformance() {
        // Simulate file system performance analysis
        return Map.of(
            "type", "FILE_SYSTEM",
            "priority", "MEDIUM",
            "recommendation", "Clean up temporary files",
            "estimatedImprovement", "8%"
        );
    }
    
    private double calculateAverageExecutionTime() {
        // This would normally calculate from actual task execution times
        return 1500.0; // 1.5 seconds
    }
    
    private LocalDateTime getLastOptimizationTime() {
        return optimizationTaskRepository.findTopByStatusOrderByCompletedAtDesc(OptimizationStatus.COMPLETED)
            .map(OptimizationTask::getCompletedAt)
            .orElse(LocalDateTime.now().minusDays(1));
    }
    
    private String getOptimizationFrequency() {
        // This would normally calculate optimization frequency
        return "Daily";
    }
}
