package com.bpdb.dms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing (created/modified stamps on entities).
 *
 * <p>This lives in its own configuration class rather than on {@code DmsApplication} on
 * purpose. Sitting on the application class, it is applied to every test slice - including
 * {@code @WebMvcTest}, which builds no JPA layer - and the context then dies with
 * "JPA metamodel must not be empty". A plain {@code @Configuration} is filtered out of
 * web slices, so controller tests load while the real application is unaffected.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
