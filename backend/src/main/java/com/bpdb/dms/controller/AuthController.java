package com.bpdb.dms.controller;

import com.bpdb.dms.dto.LoginRequest;
import com.bpdb.dms.dto.LoginResponse;
import com.bpdb.dms.dto.RegisterRequest;
import com.bpdb.dms.entity.Role;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.RoleRepository;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
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

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

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
            
            User user = userRepository.findByUsernameWithRole(loginRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setUsername(userDetails.getUsername());
            response.setRole(user.getRole() != null ? user.getRole().getName().name() : "USER");
            response.setDepartment(user.getDepartment());
            
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            // 401, not 400: the request was well-formed, the credentials were not accepted.
            // Callers cannot distinguish "you typed the wrong password" from "your payload
            // was malformed" if both come back as 400.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid username or password");
        } catch (Exception e) {
            // Anything else is our problem, not the caller's credentials. Reporting a
            // database outage as "invalid username or password" sends people hunting for
            // a password issue that does not exist.
            logger.error("Login failed for user '{}' with an unexpected error",
                    loginRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Login could not be completed. Please try again.");
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
