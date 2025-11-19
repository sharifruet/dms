package com.bpdb.dms.service;

import com.bpdb.dms.entity.Permission;
import com.bpdb.dms.entity.Role;
import com.bpdb.dms.entity.RolePermission;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.security.PermissionConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Custom UserDetailsService implementation
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameWithRole(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        Set<GrantedAuthority> authorities = buildAuthorities(user);

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(Boolean.FALSE.equals(user.getIsActive()))
                .build();
    }

    private Set<GrantedAuthority> buildAuthorities(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        Role role = user.getRole();

        if (role != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().name()));

            if (role.getRolePermissions() != null) {
                role.getRolePermissions().stream()
                        .map(RolePermission::getPermission)
                        .filter(Objects::nonNull)
                        .forEach(permission -> addPermissionAuthorities(authorities, permission));
            }
        }

        return authorities;
    }

    private void addPermissionAuthorities(Set<GrantedAuthority> authorities, Permission permission) {
        if (permission.getName() != null) {
            authorities.add(new SimpleGrantedAuthority(PermissionConstants.withPrefix(permission.getName().toUpperCase())));
        }

        if (permission.getResource() != null && permission.getAction() != null) {
            String composite = permission.getResource().toUpperCase() + "_" + permission.getAction().toUpperCase();
            authorities.add(new SimpleGrantedAuthority(PermissionConstants.withPrefix(composite)));
        }
    }
}
