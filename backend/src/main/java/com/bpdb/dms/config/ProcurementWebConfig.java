package com.bpdb.dms.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.bpdb.dms.procurement.controller.PackageScopeInterceptor;
import com.bpdb.dms.procurement.service.PackageAccessService;

/**
 * Registers the package ownership check across the procurement API (REQ-P20).
 *
 * <p>The service is taken as an {@link ObjectProvider} because {@code @WebMvcTest} picks
 * up every {@code WebMvcConfigurer} but only the controller under test and its declared
 * collaborators. A hard dependency here would make every web slice fail to start over a
 * bean it has no interest in — a test failure that says nothing about the test.
 */
@Configuration
public class ProcurementWebConfig implements WebMvcConfigurer {

    private final ObjectProvider<PackageAccessService> accessService;

    public ProcurementWebConfig(ObjectProvider<PackageAccessService> accessService) {
        this.accessService = accessService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        accessService.ifAvailable(service ->
                registry.addInterceptor(new PackageScopeInterceptor(service))
                        .addPathPatterns("/api/procurement/**"));
    }
}
