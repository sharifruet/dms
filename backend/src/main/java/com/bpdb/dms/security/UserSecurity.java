package com.bpdb.dms.security;

import com.bpdb.dms.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Helper component used by SpEL expressions to evaluate whether the current
 * authentication has the required permissions to perform user-related actions.
 */
@Component("userSecurity")
public class UserSecurity {

    private final UserRepository userRepository;

    public UserSecurity(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean canManageUsers(Authentication authentication) {
        return hasAuthority(authentication, PermissionConstants.USER_MANAGEMENT);
    }

    public boolean canAccessUser(Authentication authentication, Long userId) {
        if (authentication == null || userId == null) {
            return false;
        }

        if (canManageUsers(authentication)) {
            return true;
        }

        return userRepository.findById(userId)
                .map(user -> user.getUsername().equalsIgnoreCase(authentication.getName()))
                .orElse(false);
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
            if (authority.equals(grantedAuthority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}

