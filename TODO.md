# TODO - Remaining Implementation Tasks

## 🔴 CRITICAL PRIORITY (Must Fix Before Deployment)

### 1. Add Missing Migrations to Master Changelog
**Status**: ✅ **DONE**  
**File**: `backend/src/main/resources/db/changelog/db.changelog-master.xml`  
**Issue**: Migrations 012-016 exist but are NOT included in master changelog, so they won't run on fresh deployments.

**Action**: ✅ Added these lines before closing tag:
```xml
<include file="db/changelog/012-add-dd-roles.xml"/>
<include file="db/changelog/013-create-dd-users.xml"/>
<include file="db/changelog/014-create-document-relationships.xml"/>
<include file="db/changelog/015-create-folders.xml"/>
<include file="db/changelog/016-fix-admin-password.xml"/>
```

---

### 2. Add DD1-DD4 Roles to Role Enum
**Status**: ✅ **DONE**  
**File**: `backend/src/main/java/com/bpdb/dms/entity/Role.java`  
**Issue**: RoleType enum only has ADMIN, OFFICER, VIEWER, AUDITOR. DD1-DD4 are missing.

**Action**: ✅ Added to `RoleType` enum:
```java
DD1("Deputy Director Level 1"),
DD2("Deputy Director Level 2"),
DD3("Deputy Director Level 3"),
DD4("Deputy Director Level 4");
```

---

### 3. Update SecurityConfig for DD Roles
**Status**: ✅ **DONE**  
**File**: `backend/src/main/java/com/bpdb/dms/security/SecurityConfig.java`  
**Issue**: Upload endpoint only allows ADMIN and OFFICER. DD1-DD4 roles need access.

**Action**: ✅ Updated upload endpoint:
```java
.requestMatchers(HttpMethod.POST, "/api/documents/upload")
    .hasAnyRole("ADMIN", "OFFICER", "DD1", "DD2", "DD3", "DD4")
```

✅ Also updated OCR reprocessing:
```java
.requestMatchers(HttpMethod.POST, "/api/documents/{id}/reprocess-ocr", "/api/documents/reprocess-ocr/**")
    .hasAnyRole("ADMIN", "OFFICER", "DD1", "DD2", "DD3", "DD4")
```

---

## 🟡 HIGH PRIORITY (Should Fix Soon)

### 4. Verify Folder Integration in Frontend
**Status**: ✅ **DONE**  
**File**: `frontend/src/pages/DocumentsEnhanced.tsx`  
**Issue**: Folder components exist but integration may be incomplete.

**Action**: ✅ Completed:
- ✅ Added `FolderExplorer` component rendering
- ✅ Added folder filtering with `selectedFolderId` state
- ✅ Added folder selection in upload dialog
- ✅ Updated backend to support folderId in document listing and upload
- ✅ Added folder field to Document entity
- ✅ Updated DocumentRepository with findByFolderId method

---

### 5. Verify Document Relationships in DocumentViewer
**Status**: ⚠️ Needs Verification  
**File**: `frontend/src/components/DocumentViewer.tsx`  
**Issue**: `DocumentRelationships` component exists but may not be integrated.

**Action**:
- Add "Relationships" tab to DocumentViewer
- Import and render `DocumentRelationships` component
- Test linking/unlinking documents
- Verify relationship types display correctly

---

### 6. Implement OCR Processing Service
**Status**: ❌ Not Implemented  
**Files**: New service needed  
**Issue**: No OCR implementation found. Documents need text extraction and metadata population.

**Action**:
- Integrate Tesseract OCR library
- Create `OCRService` with text extraction
- Extract metadata (Tender No, Vendor, Date, Amount, etc.)
- Add OCR processing to `FileUploadService`
- Create OCR accuracy validation interface
- Add manual correction UI

**Requirements**: FR-010, FR-023, FR-024

---

## 🟠 MEDIUM PRIORITY (Important but Not Blocking)

### 7. Implement Duplicate Detection
**Status**: ⚠️ Partial  
**File**: `backend/src/main/java/com/bpdb/dms/service/FileUploadService.java`  
**Issue**: `Document` has `fileHash` field but no duplicate detection logic.

**Action**:
- Check file hash before upload in `FileUploadService`
- Return duplicate warning if hash exists
- Add duplicate handling options (skip, upload as version, replace)
- Create duplicate detection UI
- Add duplicate management page

**Requirements**: FR-015

---

### 8. Implement Document Archive and Restore
**Status**: ⚠️ Partial  
**Files**: `Document.java`, `DocumentService.java`, `DocumentController.java`  
**Issue**: `DocumentVersion` has `isArchived` but no document-level archiving.

**Action**:
- Add `isArchived` and `archivedAt` to `Document` entity
- Add `deletedAt` for soft delete
- Create archive/restore endpoints
- Add archive management UI
- Add restore functionality for deleted documents
- Create archive reports

**Requirements**: FR-036, FR-037

---

### 9. Implement Stationery Tracking per Employee
**Status**: ⚠️ Partial  
**Files**: `Document.java`, `DocumentService.java`  
**Issue**: `DocumentType.STATIONERY_RECORD` exists but no employee assignment.

**Action**:
- Add `assignedEmployee` field to `Document` (for STATIONERY_RECORD type)
- Add UI to assign stationery records to employees
- Create reports showing stationery per employee
- Link stationery records with asset assignments

**Requirements**: FR-017

---

## 🟢 LOW PRIORITY (Nice to Have)

### 10. End-to-End Testing
**Status**: ⚠️ Needs Testing  
**Issue**: After fixes, all features need comprehensive testing.

**Action**:
- Test DD1-DD4 role upload permissions
- Test document relationship linking/unlinking
- Test folder creation, navigation, and document filtering
- Test folder summary views
- Verify all API endpoints work correctly
- Test frontend-backend integration

---

## Summary

**Critical (Must Fix)**: 3 items  
**High Priority**: 3 items  
**Medium Priority**: 3 items  
**Low Priority**: 1 item  

**Total**: 10 items

---

*Last Updated: After pull from main*  
*Priority based on: Feature completeness, integration status, and business requirements*

