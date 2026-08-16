package com.bpdb.dms.support;

import com.bpdb.dms.entity.Role;
import com.bpdb.dms.entity.Role.RoleType;
import com.bpdb.dms.repository.RoleRepository;

/**
 * Roles for tests that talk to the database.
 *
 * <p>In a real environment the role table is seeded by Liquibase. Tests run on H2 with
 * {@code ddl-auto=create-drop} and no changelog — the procurement changesets are
 * PostgreSQL-specific — so the table starts empty. {@code User.role} is not nullable,
 * which means any test that saves a user has to put a role there first. Several tests
 * did not, and failed with {@code NULL not allowed for column "ROLE_ID"}.
 *
 * <p>Each call is idempotent, so it is safe in a {@code @BeforeEach} that runs per test.
 */
public final class TestRoles {

    private TestRoles() {
    }

    /** The role most tests want: a normal user who can upload and view documents. */
    public static Role officer(RoleRepository roleRepository) {
        return ensure(roleRepository, RoleType.OFFICER);
    }

    public static Role ensure(RoleRepository roleRepository, RoleType type) {
        return roleRepository.findByName(type).orElseGet(() -> {
            Role role = new Role();
            role.setName(type);
            role.setDisplayName(type.name());
            role.setDescription("Seeded for tests");
            return roleRepository.save(role);
        });
    }
}
