package com.bpdb.dms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * Repository scanning for the application.
 *
 * <p>Kept out of {@code DmsApplication} for the same reason as {@link JpaAuditingConfig}:
 * annotations on the application class apply to every test slice, so a
 * {@code @WebMvcTest} would try to build every JPA repository and fail with
 * "No bean named 'entityManagerFactory' available". As a plain {@code @Configuration}
 * this is filtered out of web slices, which mock the repositories they need instead.
 *
 * <p>The procurement module keeps its repositories in its own package, so it must be
 * listed explicitly - this scan is a whitelist, not a prefix match, and omitting it
 * leaves every procurement repository bean missing at startup.
 */
@Configuration
@EnableJpaRepositories(basePackages = {
        "com.bpdb.dms.repository",
        "com.bpdb.dms.procurement.repository"
})
@EnableRedisRepositories(basePackages = "com.bpdb.dms.repository.redis")
public class PersistenceConfig {
}
