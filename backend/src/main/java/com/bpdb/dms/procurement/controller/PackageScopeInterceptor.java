package com.bpdb.dms.procurement.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.bpdb.dms.procurement.service.PackageAccessService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Applies the package ownership rule to every procurement route (REQ-P20).
 *
 * <p>One place rather than a check at the top of forty controller methods: the failure
 * mode of the per-method approach is the method somebody forgets, and that method is
 * indistinguishable from the others until someone goes looking. A route that takes a
 * package id is scoped by virtue of taking one.
 *
 * <p>Deliberately not a {@code @Component}: {@code @WebMvcTest} auto-detects
 * {@code HandlerInterceptor} beans and would try to build this one — and its service, and
 * its repositories — inside a slice that has none of them. It is constructed by
 * {@code ProcurementWebConfig} instead, which knows when the service is really there.
 */
public class PackageScopeInterceptor implements HandlerInterceptor {

    private static final String PACKAGES_PREFIX = "/api/procurement/packages/";

    private final PackageAccessService accessService;

    public PackageScopeInterceptor(PackageAccessService accessService) {
        this.accessService = accessService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if (!accessService.isEnforcing()) {
            return true;
        }
        Long packageId = packageIdOf(request);
        if (packageId == null) {
            return true;
        }
        try {
            accessService.requireAccess(packageId);
            return true;
        } catch (PackageAccessService.AccessDeniedForPackage denied) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"" + denied.getMessage() + "\"}");
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Long packageIdOf(HttpServletRequest request) {
        Map<String, String> vars = (Map<String, String>)
                request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (vars == null) {
            return null;
        }
        String value = vars.get("packageId");
        if (value == null && request.getRequestURI().startsWith(PACKAGES_PREFIX)) {
            // /api/procurement/packages/{id} names its variable "id"
            value = vars.get("id");
        }
        try {
            return value == null ? null : Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null; // a non-numeric path variable is the controller's to reject
        }
    }
}
