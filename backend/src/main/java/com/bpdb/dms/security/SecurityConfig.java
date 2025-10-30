package com.bpdb.dms.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;

import java.util.Arrays;

/**
 * Security configuration for the DMS application
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/users/register").permitAll()
                // WebSocket endpoints
                .requestMatchers("/ws/**").permitAll()
                // TEMP: Allow GET list of documents and related GET subpaths
                .requestMatchers(HttpMethod.GET, "/api/documents", "/api/documents/**").permitAll()
                // Allow upload for admins and officers
                .requestMatchers(HttpMethod.POST, "/api/documents/upload").hasAnyRole("ADMIN", "OFFICER")
                // User management endpoints
                .requestMatchers("/api/users/**").hasAnyRole("ADMIN", "OFFICER")
                .requestMatchers("/api/users").hasRole("ADMIN")
                .requestMatchers("/api/users/{userId}/reset-password").hasRole("ADMIN")
                .requestMatchers("/api/users/{userId}/toggle-status").hasRole("ADMIN")
                .requestMatchers("/api/users/statistics").hasRole("ADMIN")
                // Audit log endpoints
                .requestMatchers("/api/audit/**").hasAnyRole("ADMIN", "AUDITOR")
                // Workflow endpoints
                .requestMatchers("/api/workflows/**").hasAnyRole("ADMIN", "OFFICER")
                // Document versioning endpoints
                .requestMatchers("/api/documents/*/versions/**").hasAnyRole("ADMIN", "OFFICER", "VIEWER")
                // Webhook endpoints
                .requestMatchers("/api/webhooks/**").hasRole("ADMIN")
                // Template endpoints
                .requestMatchers("/api/templates/**").hasAnyRole("ADMIN", "OFFICER")
                // Enterprise integration endpoints
                .requestMatchers("/api/integrations/**").hasAnyRole("ADMIN", "OFFICER")
                // Advanced analytics endpoints
                .requestMatchers("/api/analytics/**").hasAnyRole("ADMIN", "OFFICER", "VIEWER")
                // Machine learning endpoints
                .requestMatchers("/api/ml/**").hasAnyRole("ADMIN", "OFFICER")
                // System health monitoring endpoints
                .requestMatchers("/api/health/**").hasAnyRole("ADMIN", "OFFICER", "VIEWER")
                // Reporting endpoints
                .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "OFFICER", "VIEWER")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
