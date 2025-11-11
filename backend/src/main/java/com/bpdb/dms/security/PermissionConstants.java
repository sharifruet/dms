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

