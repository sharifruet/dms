package com.bpdb.dms.controller;

import com.bpdb.dms.dto.LoginRequest;
import com.bpdb.dms.dto.LoginResponse;
import com.bpdb.dms.dto.RegisterRequest;
import com.bpdb.dms.entity.Role;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.RoleRepository;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Authentication controller for login and registration
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private RoleRepository roleRepository;
    
    /**
     * User login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
            
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails);
            
            User user = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);
            
            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setUsername(userDetails.getUsername());
            response.setRole(user.getRole().getName().name());
            response.setDepartment(user.getDepartment());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid username or password");
        }
    }
    
    /**
     * User registration endpoint
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            if (userRepository.existsByUsername(registerRequest.getUsername())) {
                return ResponseEntity.badRequest().body("Username already exists");
            }
            
            if (userRepository.existsByEmail(registerRequest.getEmail())) {
                return ResponseEntity.badRequest().body("Email already exists");
            }
            
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setFirstName(registerRequest.getFirstName());
            user.setLastName(registerRequest.getLastName());
            // Find the role entity by name
            Role roleEntity = roleRepository.findByName(registerRequest.getRole())
                    .orElseThrow(() -> new RuntimeException("Role not found: " + registerRequest.getRole()));
            
            user.setRole(roleEntity);
            user.setDepartment(registerRequest.getDepartment());
            
            userRepository.save(user);
            
            return ResponseEntity.ok("User registered successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
    }
}
