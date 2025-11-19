# DD Role Users - Test Credentials

This document contains the test user credentials for DD1-DD4 roles created by the database migration.

## Test Users

All DD role users have been created with the following credentials:

| Username | Email | Role | Password | Department |
|----------|-------|------|----------|------------|
| dd1_user | dd1@dms.local | DD1 (Deputy Director Level 1) | admin123 | Management |
| dd2_user | dd2@dms.local | DD2 (Deputy Director Level 2) | admin123 | Management |
| dd3_user | dd3@dms.local | DD3 (Deputy Director Level 3) | admin123 | Management |
| dd4_user | dd4@dms.local | DD4 (Deputy Director Level 4) | admin123 | Management |

## Default Password

**Password for all DD users: `admin123`**

> **Note**: This is the same password as the admin user for testing convenience. In production, each user should have a unique, strong password.

## Permissions

All DD1-DD4 users have the following permissions:
- ✅ Document Upload (`DOCUMENT_UPLOAD`)
- ✅ Document View (`DOCUMENT_VIEW`)
- ✅ OCR Reprocessing (can reprocess OCR on documents)

## Testing Upload Permissions

To test that DD1-DD4 users can upload documents:

1. Login with any DD user (e.g., `dd1_user` / `admin123`)
2. Navigate to Documents page
3. Click "Upload Document" button
4. Select a file and upload
5. Verify the upload succeeds

## Changing Passwords

Users can change their passwords through the user management interface (if available) or administrators can reset passwords through the admin panel.

## Migration File

These users are created by the migration file:
- `backend/src/main/resources/db/changelog/013-create-dd-users.xml`

The migration runs automatically when the database is updated.

---

*Last Updated: [Current Date]*

