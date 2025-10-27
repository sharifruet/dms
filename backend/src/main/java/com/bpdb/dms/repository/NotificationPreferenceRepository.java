package com.bpdb.dms.repository;

import com.bpdb.dms.entity.NotificationPreference;
import com.bpdb.dms.entity.NotificationType;
import com.bpdb.dms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for NotificationPreference entity
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    
    /**
     * Find notification preferences by user
     */
    List<NotificationPreference> findByUser(User user);
    
    /**
     * Find notification preference by user and type
     */
    Optional<NotificationPreference> findByUserAndNotificationType(User user, NotificationType notificationType);
    
    /**
     * Find users with email enabled for specific notification type
     */
    @Query("SELECT np.user FROM NotificationPreference np WHERE np.notificationType = :type AND np.emailEnabled = true")
    List<User> findUsersWithEmailEnabled(@Param("type") NotificationType type);
    
    /**
     * Find users with SMS enabled for specific notification type
     */
    @Query("SELECT np.user FROM NotificationPreference np WHERE np.notificationType = :type AND np.smsEnabled = true")
    List<User> findUsersWithSmsEnabled(@Param("type") NotificationType type);
    
    /**
     * Find users with in-app notifications enabled for specific notification type
     */
    @Query("SELECT np.user FROM NotificationPreference np WHERE np.notificationType = :type AND np.inAppEnabled = true")
    List<User> findUsersWithInAppEnabled(@Param("type") NotificationType type);
    
    /**
     * Find users with push notifications enabled for specific notification type
     */
    @Query("SELECT np.user FROM NotificationPreference np WHERE np.notificationType = :type AND np.pushEnabled = true")
    List<User> findUsersWithPushEnabled(@Param("type") NotificationType type);
    
    /**
     * Check if user has specific notification type enabled
     */
    @Query("SELECT CASE WHEN COUNT(np) > 0 THEN true ELSE false END FROM NotificationPreference np WHERE np.user = :user AND np.notificationType = :type AND np.emailEnabled = true")
    boolean isEmailEnabledForUser(@Param("user") User user, @Param("type") NotificationType type);
    
    /**
     * Check if user has SMS enabled for specific notification type
     */
    @Query("SELECT CASE WHEN COUNT(np) > 0 THEN true ELSE false END FROM NotificationPreference np WHERE np.user = :user AND np.notificationType = :type AND np.smsEnabled = true")
    boolean isSmsEnabledForUser(@Param("user") User user, @Param("type") NotificationType type);
    
    /**
     * Check if user has in-app notifications enabled for specific notification type
     */
    @Query("SELECT CASE WHEN COUNT(np) > 0 THEN true ELSE false END FROM NotificationPreference np WHERE np.user = :user AND np.notificationType = :type AND np.inAppEnabled = true")
    boolean isInAppEnabledForUser(@Param("user") User user, @Param("type") NotificationType type);
}
