package com.bpdb.dms.repository;

import com.bpdb.dms.entity.Dashboard;
import com.bpdb.dms.entity.DashboardType;
import com.bpdb.dms.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Dashboard entity
 */
@Repository
public interface DashboardRepository extends JpaRepository<Dashboard, Long> {
    
    /**
     * Find dashboards by creator
     */
    Page<Dashboard> findByCreatedBy(User createdBy, Pageable pageable);
    
    /**
     * Find dashboards by type
     */
    Page<Dashboard> findByType(DashboardType type, Pageable pageable);
    
    /**
     * Find public dashboards
     */
    Page<Dashboard> findByIsPublicTrue(Pageable pageable);
    
    /**
     * Find default dashboards
     */
    List<Dashboard> findByIsDefaultTrue();
    
    /**
     * Find default dashboard by type
     */
    Dashboard findByTypeAndIsDefaultTrue(DashboardType type);
    
    /**
     * Find dashboards by name containing
     */
    Page<Dashboard> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    /**
     * Find most accessed dashboards
     */
    @Query("SELECT d FROM Dashboard d ORDER BY d.accessCount DESC")
    List<Dashboard> findMostAccessedDashboards(Pageable pageable);
    
    /**
     * Find recently accessed dashboards
     */
    @Query("SELECT d FROM Dashboard d WHERE d.lastAccessedAt IS NOT NULL ORDER BY d.lastAccessedAt DESC")
    List<Dashboard> findRecentlyAccessedDashboards(Pageable pageable);
    
    /**
     * Count dashboards by type
     */
    long countByType(DashboardType type);
    
    /**
     * Find dashboards accessible to user (public or created by user)
     */
    @Query("SELECT d FROM Dashboard d WHERE d.isPublic = true OR d.createdBy = :user")
    Page<Dashboard> findAccessibleToUser(@Param("user") User user, Pageable pageable);
}
