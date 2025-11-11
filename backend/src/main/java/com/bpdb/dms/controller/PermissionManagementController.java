package com.bpdb.dms.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bpdb.dms.dto.PermissionDto;
import com.bpdb.dms.service.RoleManagementService;

/**
 * Controller exposing permission catalogue for administrative clients.
 */
@RestController
@RequestMapping("/api/permissions")
@CrossOrigin(origins = "*")
public class PermissionManagementController {
    
    @Autowired
    private RoleManagementService roleManagementService;
    
    @GetMapping
    @PreAuthorize("@userSecurity.canManageUsers(authentication)")
    public ResponseEntity<List<PermissionDto>> getAllPermissions() {
        List<PermissionDto> permissions = roleManagementService.getAllPermissions().stream()
                .map(PermissionDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(permissions);
    }
}

