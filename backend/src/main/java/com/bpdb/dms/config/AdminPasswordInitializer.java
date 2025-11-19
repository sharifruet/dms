package com.bpdb.dms.config;

import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Initializes admin password on application startup
 * This ensures the admin user has the correct password hash
 */
@Component
public class AdminPasswordInitializer {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminPasswordInitializer.class);
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeAdminPassword() {
        try {
            Optional<User> adminUserOpt = userRepository.findByUsername(ADMIN_USERNAME);
            if (adminUserOpt.isPresent()) {
                User adminUser = adminUserOpt.get();
                // Verify if current password hash matches
                if (!passwordEncoder.matches(ADMIN_PASSWORD, adminUser.getPassword())) {
                    // Update with correct hash
                    String correctHash = passwordEncoder.encode(ADMIN_PASSWORD);
                    adminUser.setPassword(correctHash);
                    userRepository.save(adminUser);
                    logger.info("Admin password hash updated successfully");
                } else {
                    logger.info("Admin password hash is already correct");
                }
            } else {
                logger.warn("Admin user not found - should be created by Liquibase migrations");
            }
        } catch (Exception e) {
            logger.error("Failed to initialize admin password: {}", e.getMessage(), e);
        }
    }
}

