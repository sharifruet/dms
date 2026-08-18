package com.bpdb.dms.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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

    /**
     * Keep the JWT filter out of the servlet container's own filter chain.
     *
     * <p>{@link JwtAuthenticationFilter} is a {@code @Component}, so Spring Boot registers
     * it with the container automatically - on top of the {@code addFilterBefore} below.
     * The request therefore met it twice, and because it extends {@code OncePerRequestFilter}
     * only the first copy ever ran. That copy sits outside the security chain, so the
     * authentication it established was wiped by {@code SecurityContextHolderFilter}, and
     * the copy inside the chain skipped itself as already-filtered.
     *
     * <p>The effect was that every authority-gated endpoint returned an empty 403 to a
     * correctly authenticated caller: the filter logged "JWT Auth Success", and
     * authorization a few filters later saw an anonymous user. Registering the bean here
     * with {@code setEnabled(false)} leaves exactly one copy, the one in the chain.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterNotInServletChain(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
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
                // WebSocket endpoints - must come early to allow handshake requests
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                // When a handler throws, the container re-dispatches to /error. That
                // dispatch carries no authentication, so with /error secured every 500
                // reached the client as an empty 403 - the status said "you may not",
                // the truth was "it broke". A listing failure spent an afternoon looking
                // like a permissions problem for exactly this reason.
                .requestMatchers("/error").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/users/register").permitAll()
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

                // Procurement lifecycle - Maker / Checker (client answer Q-17, REQ-X4).
                // The Maker captures; the Checker approves. Matchers are ordered most
                // specific first, because Spring Security takes the first one that matches.
                //
                // Gated on permissions rather than role names on purpose. A user holds
                // exactly one role, so naming MAKER and CHECKER here would force every
                // existing OFFICER or DD account to give up the role that grants its
                // document access before it could open a package. Changeset 042 grants the
                // capture permissions to those roles instead, so both can be true at once.
                //
                // Approval: completing a stage, declaring it Not Applicable, sending it
                // back for rework, opening a new tender attempt, and setting budget.
                .requestMatchers(HttpMethod.POST, "/api/procurement/packages/*/stages/*/complete")
                    .hasAuthority(PermissionConstants.PROCUREMENT_VERIFY)
                .requestMatchers(HttpMethod.POST, "/api/procurement/packages/*/stages/*/not-applicable")
                    .hasAuthority(PermissionConstants.PROCUREMENT_OVERRIDE)
                .requestMatchers(HttpMethod.POST, "/api/procurement/packages/*/stages/*/rework")
                    .hasAuthority(PermissionConstants.PROCUREMENT_OVERRIDE)
                .requestMatchers(HttpMethod.POST, "/api/procurement/packages/*/stages/2/re-tender")
                    .hasAuthority(PermissionConstants.PROCUREMENT_OVERRIDE)
                // Declaring a delivery final closes the delivery set and unblocks the
                // stage, so it is an approval rather than data entry (REQ-12.5)
                .requestMatchers(HttpMethod.POST, "/api/procurement/packages/*/stages/12/deliveries/*/final")
                    .hasAuthority(PermissionConstants.PROCUREMENT_VERIFY)
                .requestMatchers(HttpMethod.POST, "/api/procurement/packages/*/stages/12/deliveries/*/reopen")
                    .hasAuthority(PermissionConstants.PROCUREMENT_OVERRIDE)
                .requestMatchers(HttpMethod.POST, "/api/procurement/packages/*/budget")
                    .hasAuthority(PermissionConstants.BUDGET_APPROVE)
                .requestMatchers(HttpMethod.POST, "/api/procurement/department-budgets")
                    .hasAuthority(PermissionConstants.BUDGET_APPROVE)
                // Deleting retained history is an administrator's act, not a Checker's
                // (REQ-P17). Nothing else in procurement destroys anything.
                .requestMatchers(HttpMethod.POST, "/api/procurement/retention/purge")
                    .hasRole("ADMIN")
                // Migration rewrites the store wholesale; an administrator's job, not a
                // Checker's, and one that happens once per environment
                .requestMatchers(HttpMethod.POST, "/api/procurement/migration/**")
                    .hasRole("ADMIN")
                // Reading is open to anyone who can see the module at all
                .requestMatchers(HttpMethod.GET, "/api/procurement/**")
                    .hasAuthority(PermissionConstants.PROCUREMENT_VIEW)
                // Everything else under procurement is capture, which a Maker may do
                .requestMatchers("/api/procurement/**")
                    .hasAuthority(PermissionConstants.PROCUREMENT_CAPTURE)
                // User management endpoints
                .requestMatchers("/api/users/**").hasAuthority(PermissionConstants.USER_MANAGEMENT)
                .requestMatchers("/api/roles/**").hasAuthority(PermissionConstants.USER_MANAGEMENT)
                .requestMatchers("/api/permissions/**").hasAuthority(PermissionConstants.USER_MANAGEMENT)
                // Audit log endpoints
                .requestMatchers("/api/audit/**").hasAuthority(PermissionConstants.AUDIT_VIEW)
                // Workflow endpoints - allow all authenticated users to GET workflow data (must come before general workflow matcher)
                .requestMatchers(HttpMethod.GET, "/api/workflows/**").authenticated()
                .requestMatchers("/api/workflows/**").hasAnyRole("ADMIN", "OFFICER")
                // Document type fields endpoints
                .requestMatchers(HttpMethod.GET, "/api/document-type-fields/**").hasAnyRole("ADMIN", "OFFICER", "VIEWER")
                .requestMatchers("/api/document-type-fields/**").hasAnyRole("ADMIN", "OFFICER")
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
        // Use allowedOriginPatterns for wildcard support with credentials
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // Cache preflight for 1 hour
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
