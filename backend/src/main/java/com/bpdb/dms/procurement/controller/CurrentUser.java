package com.bpdb.dms.procurement.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.bpdb.dms.repository.UserRepository;

/**
 * Resolves the acting user for audit and verification stamps.
 *
 * Authentication carries a username; the capture and stage records want a user id, so
 * the lookup happens here rather than being repeated in every controller.
 */
@Component
public class CurrentUser {

    private static UserRepository userRepository;

    public CurrentUser(UserRepository userRepository) {
        CurrentUser.userRepository = userRepository;
    }

    /** The acting user's id, or null when the call is unauthenticated. */
    public static Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || userRepository == null) {
            return null;
        }
        return userRepository.findByUsername(auth.getName())
                .map(com.bpdb.dms.entity.User::getId)
                .orElse(null);
    }

    public static String username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }
}
