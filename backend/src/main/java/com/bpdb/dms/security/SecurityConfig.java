package com.bpdb.dms.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/users/register").permitAll()
                // WebSocket endpoints
                .requestMatchers("/ws/**").permitAll()
                // Document endpoints
                .requestMatchers(HttpMethod.GET, "/api/documents/**").hasAuthority(PermissionConstants.DOCUMENT_VIEW)
                .requestMatchers(HttpMethod.GET, "/api/document-categories/**").hasAuthority(PermissionConstants.DOCUMENT_VIEW)
                .requestMatchers(HttpMethod.POST, "/api/documents/upload").hasAnyRole("ADMIN", "OFFICER", "DD1", "DD2", "DD3", "DD4")
                .requestMatchers(HttpMethod.POST, "/api/documents/{id}/reprocess-ocr", "/api/documents/reprocess-ocr/**").hasAnyRole("ADMIN", "OFFICER", "DD1", "DD2", "DD3", "DD4")
                .requestMatchers(HttpMethod.DELETE, "/api/documents/**").hasAuthority(PermissionConstants.DOCUMENT_DELETE)
                // Smart Folder (DMC) endpoints
                .requestMatchers(HttpMethod.POST, "/api/dmc/folders/**").hasAnyRole("ADMIN", "OFFICER")
                .requestMatchers(HttpMethod.PUT, "/api/dmc/folders/**").hasAnyRole("ADMIN", "OFFICER")
                .requestMatchers(HttpMethod.DELETE, "/api/dmc/folders/**").hasAnyRole("ADMIN", "OFFICER")
                .requestMatchers(HttpMethod.GET, "/api/dmc/folders/**").hasAnyRole("ADMIN", "OFFICER", "VIEWER")
                // Finance endpoints
                .requestMatchers(HttpMethod.POST, "/api/finance/app/import").hasAnyRole("ADMIN", "OFFICER")
                .requestMatchers(HttpMethod.POST, "/api/finance/bills").hasAnyRole("ADMIN", "OFFICER")
                .requestMatchers(HttpMethod.GET, "/api/finance/**").hasAnyRole("ADMIN", "OFFICER", "VIEWER")
                // Search endpoints
                .requestMatchers("/api/search/**").hasAnyRole("ADMIN", "OFFICER", "VIEWER")
                // User management endpoints
                .requestMatchers("/api/users/**").hasAuthority(PermissionConstants.USER_MANAGEMENT)
                .requestMatchers("/api/roles/**").hasAuthority(PermissionConstants.USER_MANAGEMENT)
                .requestMatchers("/api/permissions/**").hasAuthority(PermissionConstants.USER_MANAGEMENT)
                // Audit log endpoints
                .requestMatchers("/api/audit/**").hasAuthority(PermissionConstants.AUDIT_VIEW)
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
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler(accessDeniedHandler())
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) -> {
            System.out.println("Access Denied - URI: " + request.getRequestURI() + ", Method: " + request.getMethod());
            System.out.println("Authentication: " + org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
            if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null) {
                System.out.println("Authorities: " + org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities());
            }
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Access Denied\",\"message\":\"" + accessDeniedException.getMessage() + "\"}");
        };
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
