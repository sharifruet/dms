package com.bpdb.dms.entity;

/**
 * Health check types
 */
public enum HealthCheckType {
    DATABASE_CONNECTION("Database Connection"),
    REDIS_CONNECTION("Redis Connection"),
    ELASTICSEARCH_CONNECTION("Elasticsearch Connection"),
    FILE_SYSTEM("File System"),
    MEMORY_USAGE("Memory Usage"),
    CPU_USAGE("CPU Usage"),
    DISK_SPACE("Disk Space"),
    NETWORK_CONNECTIVITY("Network Connectivity"),
    API_RESPONSE_TIME("API Response Time"),
    SERVICE_AVAILABILITY("Service Availability"),
    DEPENDENCY_CHECK("Dependency Check"),
    SECURITY_CHECK("Security Check"),
    BACKUP_STATUS("Backup Status"),
    INTEGRATION_STATUS("Integration Status"),
    CUSTOM_CHECK("Custom Check");
    
    private final String displayName;
    
    HealthCheckType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
