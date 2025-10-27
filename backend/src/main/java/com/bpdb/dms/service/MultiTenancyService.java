package com.bpdb.dms.service;

import com.bpdb.dms.entity.Tenant;
import com.bpdb.dms.entity.TenantPlan;
import com.bpdb.dms.entity.TenantStatus;
import com.bpdb.dms.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for multi-tenant management
 */
@Service
@Transactional
public class MultiTenancyService {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiTenancyService.class);
    
    @Autowired
    private TenantRepository tenantRepository;
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Create a new tenant
     */
    public Tenant createTenant(String tenantName, String tenantCode, TenantPlan plan,
                               String contactEmail, String contactName) {
        try {
            logger.info("Creating new tenant: {}", tenantName);
            
            Tenant tenant = new Tenant(tenantName, tenantCode, plan);
            tenant.setContactEmail(contactEmail);
            tenant.setContactName(contactName);
            tenant.setSubscriptionStartDate(LocalDateTime.now());
            
            // Set plan-specific limits
            setPlanLimits(tenant, plan);
            
            Tenant savedTenant = tenantRepository.save(tenant);
            
            auditService.logActivity("SYSTEM", "TENANT_CREATED", 
                "Tenant created: " + tenantName, null);
            
            logger.info("Tenant created successfully: {}", tenantName);
            
            return savedTenant;
            
        } catch (Exception e) {
            logger.error("Failed to create tenant: {}", e.getMessage());
            throw new RuntimeException("Failed to create tenant", e);
        }
    }
    
    /**
     * Get tenant by ID
     */
    public Tenant getTenantById(Long tenantId) {
        return tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Tenant not found"));
    }
    
    /**
     * Get tenant by tenant code
     */
    public Tenant getTenantByCode(String tenantCode) {
        return tenantRepository.findByTenantCode(tenantCode)
            .orElseThrow(() -> new RuntimeException("Tenant not found"));
    }
    
    /**
     * Get tenant by domain
     */
    public Tenant getTenantByDomain(String domain) {
        return tenantRepository.findByDomain(domain)
            .orElseThrow(() -> new RuntimeException("Tenant not found"));
    }
    
    /**
     * Get tenant by subdomain
     */
    public Tenant getTenantBySubdomain(String subdomain) {
        return tenantRepository.findBySubdomain(subdomain)
            .orElseThrow(() -> new RuntimeException("Tenant not found"));
    }
    
    /**
     * Update tenant
     */
    public Tenant updateTenant(Long tenantId, Tenant tenantUpdates) {
        try {
            Tenant existingTenant = getTenantById(tenantId);
            
            // Update allowed fields
            if (tenantUpdates.getTenantName() != null) {
                existingTenant.setTenantName(tenantUpdates.getTenantName());
            }
            if (tenantUpdates.getContactEmail() != null) {
                existingTenant.setContactEmail(tenantUpdates.getContactEmail());
            }
            if (tenantUpdates.getContactName() != null) {
                existingTenant.setContactName(tenantUpdates.getContactName());
            }
            if (tenantUpdates.getContactPhone() != null) {
                existingTenant.setContactPhone(tenantUpdates.getContactPhone());
            }
            if (tenantUpdates.getAddress() != null) {
                existingTenant.setAddress(tenantUpdates.getAddress());
            }
            if (tenantUpdates.getCity() != null) {
                existingTenant.setCity(tenantUpdates.getCity());
            }
            if (tenantUpdates.getState() != null) {
                existingTenant.setState(tenantUpdates.getState());
            }
            if (tenantUpdates.getCountry() != null) {
                existingTenant.setCountry(tenantUpdates.getCountry());
            }
            if (tenantUpdates.getPostalCode() != null) {
                existingTenant.setPostalCode(tenantUpdates.getPostalCode());
            }
            if (tenantUpdates.getTimezone() != null) {
                existingTenant.setTimezone(tenantUpdates.getTimezone());
            }
            if (tenantUpdates.getLocale() != null) {
                existingTenant.setLocale(tenantUpdates.getLocale());
            }
            if (tenantUpdates.getConfiguration() != null) {
                existingTenant.setConfiguration(tenantUpdates.getConfiguration());
            }
            
            existingTenant.setUpdatedAt(LocalDateTime.now());
            
            Tenant updatedTenant = tenantRepository.save(existingTenant);
            
            auditService.logActivity("SYSTEM", "TENANT_UPDATED", 
                "Tenant updated: " + existingTenant.getTenantName(), null);
            
            logger.info("Tenant updated successfully: {}", existingTenant.getTenantName());
            
            return updatedTenant;
            
        } catch (Exception e) {
            logger.error("Failed to update tenant: {}", e.getMessage());
            throw new RuntimeException("Failed to update tenant", e);
        }
    }
    
    /**
     * Suspend tenant
     */
    public Tenant suspendTenant(Long tenantId, String reason) {
        try {
            Tenant tenant = getTenantById(tenantId);
            tenant.setStatus(TenantStatus.SUSPENDED);
            tenant.setUpdatedAt(LocalDateTime.now());
            
            Tenant suspendedTenant = tenantRepository.save(tenant);
            
            auditService.logActivity("SYSTEM", "TENANT_SUSPENDED", 
                "Tenant suspended: " + tenant.getTenantName() + " - Reason: " + reason, null);
            
            logger.info("Tenant suspended: {}", tenant.getTenantName());
            
            return suspendedTenant;
            
        } catch (Exception e) {
            logger.error("Failed to suspend tenant: {}", e.getMessage());
            throw new RuntimeException("Failed to suspend tenant", e);
        }
    }
    
    /**
     * Activate tenant
     */
    public Tenant activateTenant(Long tenantId) {
        try {
            Tenant tenant = getTenantById(tenantId);
            tenant.setStatus(TenantStatus.ACTIVE);
            tenant.setUpdatedAt(LocalDateTime.now());
            
            Tenant activatedTenant = tenantRepository.save(tenant);
            
            auditService.logActivity("SYSTEM", "TENANT_ACTIVATED", 
                "Tenant activated: " + tenant.getTenantName(), null);
            
            logger.info("Tenant activated: {}", tenant.getTenantName());
            
            return activatedTenant;
            
        } catch (Exception e) {
            logger.error("Failed to activate tenant: {}", e.getMessage());
            throw new RuntimeException("Failed to activate tenant", e);
        }
    }
    
    /**
     * Upgrade tenant plan
     */
    public Tenant upgradeTenantPlan(Long tenantId, TenantPlan newPlan) {
        try {
            Tenant tenant = getTenantById(tenantId);
            TenantPlan oldPlan = tenant.getPlan();
            
            tenant.setPlan(newPlan);
            setPlanLimits(tenant, newPlan);
            tenant.setUpdatedAt(LocalDateTime.now());
            
            Tenant upgradedTenant = tenantRepository.save(tenant);
            
            auditService.logActivity("SYSTEM", "TENANT_PLAN_UPGRADED", 
                "Tenant plan upgraded: " + tenant.getTenantName() + " from " + oldPlan + " to " + newPlan, null);
            
            logger.info("Tenant plan upgraded: {} from {} to {}", tenant.getTenantName(), oldPlan, newPlan);
            
            return upgradedTenant;
            
        } catch (Exception e) {
            logger.error("Failed to upgrade tenant plan: {}", e.getMessage());
            throw new RuntimeException("Failed to upgrade tenant plan", e);
        }
    }
    
    /**
     * Get all tenants
     */
    public Page<Tenant> getAllTenants(TenantStatus status, TenantPlan plan, Pageable pageable) {
        return tenantRepository.findByMultipleCriteria(status, plan, pageable);
    }
    
    /**
     * Get tenant statistics
     */
    public Map<String, Object> getTenantStatistics() {
        try {
            return Map.of(
                "totalTenants", tenantRepository.count(),
                "activeTenants", tenantRepository.countByStatus(TenantStatus.ACTIVE),
                "suspendedTenants", tenantRepository.countByStatus(TenantStatus.SUSPENDED),
                "trialTenants", tenantRepository.countByStatus(TenantStatus.TRIAL),
                "expiredTenants", tenantRepository.countByStatus(TenantStatus.EXPIRED),
                "totalUsers", tenantRepository.sumCurrentUsers(),
                "totalStorageBytes", tenantRepository.sumCurrentStorageBytes(),
                "averageUsersPerTenant", calculateAverageUsersPerTenant(),
                "averageStoragePerTenant", calculateAverageStoragePerTenant()
            );
            
        } catch (Exception e) {
            logger.error("Failed to get tenant statistics: {}", e.getMessage());
            throw new RuntimeException("Failed to get tenant statistics", e);
        }
    }
    
    /**
     * Check tenant limits
     */
    public Map<String, Object> checkTenantLimits(Long tenantId) {
        try {
            Tenant tenant = getTenantById(tenantId);
            
            boolean userLimitExceeded = tenant.getCurrentUsers() >= tenant.getMaxUsers();
            boolean storageLimitExceeded = tenant.getCurrentStorageBytes() >= (tenant.getMaxStorageGb() * 1024 * 1024 * 1024);
            
            return Map.of(
                "tenantId", tenantId,
                "userLimitExceeded", userLimitExceeded,
                "storageLimitExceeded", storageLimitExceeded,
                "currentUsers", tenant.getCurrentUsers(),
                "maxUsers", tenant.getMaxUsers(),
                "currentStorageBytes", tenant.getCurrentStorageBytes(),
                "maxStorageBytes", tenant.getMaxStorageGb() * 1024 * 1024 * 1024,
                "usagePercentage", calculateUsagePercentage(tenant)
            );
            
        } catch (Exception e) {
            logger.error("Failed to check tenant limits: {}", e.getMessage());
            throw new RuntimeException("Failed to check tenant limits", e);
        }
    }
    
    /**
     * Scheduled task to check expired tenants
     */
    @Scheduled(fixedDelay = 3600000) // Run every hour
    public void checkExpiredTenants() {
        try {
            logger.info("Checking for expired tenants");
            
            List<Tenant> expiredTenants = tenantRepository.findExpiredTenants(LocalDateTime.now());
            
            for (Tenant tenant : expiredTenants) {
                tenant.setStatus(TenantStatus.EXPIRED);
                tenantRepository.save(tenant);
                
                auditService.logActivity("SYSTEM", "TENANT_EXPIRED", 
                    "Tenant expired: " + tenant.getTenantName(), null);
                
                logger.info("Tenant expired: {}", tenant.getTenantName());
            }
            
        } catch (Exception e) {
            logger.error("Failed to check expired tenants: {}", e.getMessage());
        }
    }
    
    /**
     * Scheduled task to update tenant usage statistics
     */
    @Scheduled(fixedDelay = 300000) // Run every 5 minutes
    public void updateTenantUsageStatistics() {
        try {
            logger.debug("Updating tenant usage statistics");
            
            List<Tenant> tenants = tenantRepository.findAll();
            
            for (Tenant tenant : tenants) {
                // Update user count
                int userCount = getUserCountForTenant(tenant.getId());
                tenant.setCurrentUsers(userCount);
                
                // Update storage usage
                long storageBytes = getStorageUsageForTenant(tenant.getId());
                tenant.setCurrentStorageBytes(storageBytes);
                
                tenantRepository.save(tenant);
            }
            
        } catch (Exception e) {
            logger.error("Failed to update tenant usage statistics: {}", e.getMessage());
        }
    }
    
    // Private helper methods
    
    private void setPlanLimits(Tenant tenant, TenantPlan plan) {
        switch (plan) {
            case FREE:
                tenant.setMaxUsers(5);
                tenant.setMaxStorageGb(1L);
                break;
            case BASIC:
                tenant.setMaxUsers(25);
                tenant.setMaxStorageGb(10L);
                break;
            case PROFESSIONAL:
                tenant.setMaxUsers(100);
                tenant.setMaxStorageGb(100L);
                break;
            case ENTERPRISE:
                tenant.setMaxUsers(1000);
                tenant.setMaxStorageGb(1000L);
                break;
            case CUSTOM:
                // Custom limits will be set manually
                break;
        }
    }
    
    private double calculateAverageUsersPerTenant() {
        try {
            long totalUsers = tenantRepository.sumCurrentUsers();
            long totalTenants = tenantRepository.count();
            return totalTenants > 0 ? (double) totalUsers / totalTenants : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private double calculateAverageStoragePerTenant() {
        try {
            long totalStorage = tenantRepository.sumCurrentStorageBytes();
            long totalTenants = tenantRepository.count();
            return totalTenants > 0 ? (double) totalStorage / totalTenants : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private double calculateUsagePercentage(Tenant tenant) {
        try {
            long maxStorageBytes = tenant.getMaxStorageGb() * 1024 * 1024 * 1024;
            return maxStorageBytes > 0 ? (double) tenant.getCurrentStorageBytes() / maxStorageBytes * 100 : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private int getUserCountForTenant(Long tenantId) {
        // This would normally query the user repository for tenant-specific users
        // For now, return a simulated count
        return 10;
    }
    
    private long getStorageUsageForTenant(Long tenantId) {
        // This would normally calculate storage usage for the tenant
        // For now, return a simulated value
        return 1024 * 1024 * 1024; // 1GB
    }
}
