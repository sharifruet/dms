package com.bpdb.dms.procurement;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Base for tests that need the <em>real</em> schema: PostgreSQL, built by Liquibase.
 *
 * <p>The rest of the suite runs on H2 with {@code ddl-auto=create-drop}, which generates
 * the schema from the entities. That is fast and fine for logic, but it means the schema
 * and the entities can never disagree — so it is structurally blind to a changeset that
 * has drifted from the code. Three defects reached a running system through that blind
 * spot, including one that made every field capture in the application fail.
 *
 * <p>Subclasses get a throwaway PostgreSQL container with the full changelog applied,
 * exactly as production would have it.
 */
@Testcontainers
@SpringBootTest
@Tag("postgres")
public abstract class PostgresLiquibaseTest {

    // One container for every test that extends this, started once per JVM. Reusing it
    // keeps the suite quick; each subclass is responsible for its own data.
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("dms_schema_check")
                    .withUsername("dms_test")
                    .withPassword("dms_test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // The point of the exercise: the schema comes from the changelog, not from the
        // entities. Hibernate must not be allowed to create or alter anything, or the
        // difference we are looking for would be papered over.
        //
        // "none" rather than "validate" deliberately. Validate fails the context on the
        // first disagreement it meets, which means one legacy type mismatch hides every
        // other problem; LiquibaseSchemaAgreementTest reports them all in one pass instead.
        // Once the existing drift is cleared, switching this to "validate" would make the
        // check automatic for every test that touches PostgreSQL.
        registry.add("spring.liquibase.enabled", () -> "true");
        registry.add("spring.liquibase.change-log", () -> "classpath:db/changelog/db.changelog-master.xml");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");

        // These run on the default profile, which no longer carries a signing key: it is
        // taken from the environment now, and the application refuses to start without one.
        // Supplying a throwaway key here keeps that check honest rather than weakening it.
        registry.add("jwt.secret", () -> "test-only-signing-key-not-used-outside-this-suite");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    /** Marks a test as needing the container; kept for readability at the call site. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface NeedsRealSchema {
    }
}
