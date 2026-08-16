package com.bpdb.dms.security;

/**
 * Centralised permission names used across the application.
 *
 * All granted authorities for permissions are prefixed with {@code PERM_}.
 * These constants mirror the values seeded via Liquibase in
 * {@code 002-create-roles-and-permissions.xml}.
 */
public final class PermissionConstants {

    private static final String PREFIX = "PERM_";

    private PermissionConstants() {
    }

    public static final String USER_MANAGEMENT = PREFIX + "USER_MANAGEMENT";
    public static final String DOCUMENT_UPLOAD = PREFIX + "DOCUMENT_UPLOAD";
    public static final String DOCUMENT_VIEW = PREFIX + "DOCUMENT_VIEW";
    public static final String DOCUMENT_DELETE = PREFIX + "DOCUMENT_DELETE";
    public static final String AUDIT_VIEW = PREFIX + "AUDIT_VIEW";

    /**
     * Procurement, expressed as permissions rather than roles (Q-17).
     *
     * <p>A user holds exactly one role, so gating the module on the MAKER and CHECKER
     * roles alone would mean every existing OFFICER or DD account had to give up the role
     * that grants its document access before it could reach procurement at all. Granting
     * these permissions to a role instead lets the two coexist, which is what changeset
     * 042 relies on.
     */
    public static final String PROCUREMENT_VIEW = PREFIX + "PROCUREMENT_VIEW";
    public static final String PROCUREMENT_CAPTURE = PREFIX + "PROCUREMENT_CAPTURE";
    public static final String PROCUREMENT_VERIFY = PREFIX + "PROCUREMENT_VERIFY";
    public static final String PROCUREMENT_OVERRIDE = PREFIX + "PROCUREMENT_OVERRIDE";
    public static final String BUDGET_APPROVE = PREFIX + "BUDGET_APPROVE";

    /**
    * Convenience method to prefix arbitrary permission names that may be stored
    * in configuration or database records.
    */
    public static String withPrefix(String permissionName) {
        if (permissionName == null || permissionName.isBlank()) {
            throw new IllegalArgumentException("Permission name must not be empty");
        }
        return permissionName.startsWith(PREFIX) ? permissionName : PREFIX + permissionName;
    }
}

