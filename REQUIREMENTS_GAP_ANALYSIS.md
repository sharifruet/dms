# Requirements Gap Analysis - DMS Implementation

## Executive Summary

This document identifies gaps between the documented requirements and the current implementation, highlighting features that need to be implemented or modified.

---

## Critical Missing Features

### 1. DD1-DD4 Role-Based Upload System (FR-005, FR-006)

**Status**: ❌ **NOT IMPLEMENTED**

**Requirement**:
- System shall enforce role-based upload permissions for:
  - DD1 (Deputy Director Level 1)
  - DD2 (Deputy Director Level 2)
  - DD3 (Deputy Director Level 3)
  - DD4 (Deputy Director Level 4)

**Current State**:
- Only ADMIN, OFFICER, VIEWER, AUDITOR roles exist
- No DD1-DD4 roles defined in `Role.java`
- Upload permissions only check for ADMIN/OFFICER roles

**Implementation Required**:
1. Add DD1, DD2, DD3, DD4 to `RoleType` enum in `Role.java`
2. Create database migration to add these roles
3. Update `SecurityConfig.java` to enforce DD1-DD4 upload permissions
4. Update frontend to show role-specific upload restrictions
5. Add role hierarchy/permission matrix for DD1-DD4

**Files to Modify**:
- `backend/src/main/java/com/bpdb/dms/entity/Role.java`
- `backend/src/main/java/com/bpdb/dms/security/SecurityConfig.java`
- `backend/src/main/resources/db/changelog/002-create-roles-and-permissions.xml`
- `frontend/src/pages/DocumentsEnhanced.tsx`

---

### 2. Document Linking/Relationship System (FR-034, FR-035, FR-096)

**Status**: ❌ **NOT IMPLEMENTED**

**Requirement**:
- System shall support linked document views:
  - Contract ↔ Letter of Credit
  - Letter of Credit ↔ Bank Guarantee
  - Bank Guarantee ↔ Purchase Order
  - Purchase Order ↔ Correspondence
- System shall maintain relationship integrity

**Current State**:
- `Document` entity has no relationship fields
- No `DocumentRelationship` entity exists
- No API endpoints for linking documents
- No UI for viewing/managing document relationships

**Implementation Required**:
1. Create `DocumentRelationship` entity with:
   - `sourceDocument` (ManyToOne)
   - `targetDocument` (ManyToOne)
   - `relationshipType` (enum: CONTRACT_TO_LC, LC_TO_BG, BG_TO_PO, PO_TO_CORRESPONDENCE)
   - `createdBy`, `createdAt`
2. Create `DocumentRelationshipRepository`
3. Create `DocumentRelationshipService` with methods:
   - `linkDocuments(Long sourceId, Long targetId, RelationshipType type)`
   - `getLinkedDocuments(Long documentId)`
   - `removeRelationship(Long relationshipId)`
4. Create `DocumentRelationshipController` with REST endpoints
5. Add relationship visualization in frontend document detail view
6. Add relationship management UI (link/unlink documents)

**Files to Create**:
- `backend/src/main/java/com/bpdb/dms/entity/DocumentRelationship.java`
- `backend/src/main/java/com/bpdb/dms/repository/DocumentRelationshipRepository.java`
- `backend/src/main/java/com/bpdb/dms/service/DocumentRelationshipService.java`
- `backend/src/main/java/com/bpdb/dms/controller/DocumentRelationshipController.java`
- `frontend/src/components/DocumentRelationships.tsx`

**Files to Modify**:
- `backend/src/main/java/com/bpdb/dms/entity/Document.java` (add relationship helper methods)
- `frontend/src/pages/DocumentsEnhanced.tsx` (add relationship UI)

---

### 3. Folder Structure and Organization (FR-031, FR-032, FR-039, FR-040, FR-041)

**Status**: ❌ **NOT IMPLEMENTED**

**Requirement**:
- System shall support folder-wise categorization
- System shall support department-wise organization
- System shall provide folder summary views (total files, uploaded files, remaining uploads)
- System shall provide explorer-style folder interface

**Current State**:
- No `Folder` entity exists
- Documents only have `department` field (string)
- No folder hierarchy or organization
- No folder summary functionality

**Implementation Required**:
1. Create `Folder` entity with:
   - `name`, `description`
   - `parentFolder` (self-referencing for hierarchy)
   - `department` (ManyToOne or String)
   - `createdBy`, `createdAt`
   - `folderPath` (computed field for full path)
2. Add `folder` field to `Document` entity (ManyToOne)
3. Create `FolderRepository` with methods for:
   - Finding by path
   - Getting children folders
   - Getting folder tree
4. Create `FolderService` with:
   - `createFolder()`
   - `getFolderTree()`
   - `getFolderSummary()` (total files, uploaded, remaining)
   - `moveDocumentToFolder()`
5. Create `FolderController` with REST endpoints
6. Create frontend folder explorer component
7. Add folder navigation breadcrumbs
8. Implement folder summary dashboard

**Files to Create**:
- `backend/src/main/java/com/bpdb/dms/entity/Folder.java`
- `backend/src/main/java/com/bpdb/dms/repository/FolderRepository.java`
- `backend/src/main/java/com/bpdb/dms/service/FolderService.java`
- `backend/src/main/java/com/bpdb/dms/controller/FolderController.java`
- `frontend/src/components/FolderExplorer.tsx`
- `frontend/src/components/FolderTree.tsx`

**Files to Modify**:
- `backend/src/main/java/com/bpdb/dms/entity/Document.java` (add folder relationship)
- `backend/src/main/resources/db/changelog/` (add folder table migration)
- `frontend/src/pages/DocumentsEnhanced.tsx` (add folder navigation)

---

### 4. Dual Start Logic for Contracts (FR-097)

**Status**: ❌ **NOT IMPLEMENTED**

**Requirement**:
- System shall support dual start logic:
  - Sign Date activation
  - LC Opening Date activation

**Current State**:
- `ExpiryTracking` entity has single `expiryDate`
- No support for dual activation dates
- No logic to handle contract timeline based on sign date vs LC opening date

**Implementation Required**:
1. Add fields to `ExpiryTracking` or create `ContractTimeline` entity:
   - `signDate` (LocalDateTime)
   - `lcOpeningDate` (LocalDateTime)
   - `activationType` (enum: SIGN_DATE, LC_OPENING_DATE)
   - `calculatedStartDate` (computed based on activation type)
2. Update `ExpiryTrackingService` to:
   - Calculate expiry based on activation type
   - Support both sign date and LC opening date scenarios
3. Add UI for selecting activation type when creating contract tracking
4. Update expiry calculations to use appropriate start date

**Files to Modify**:
- `backend/src/main/java/com/bpdb/dms/entity/ExpiryTracking.java`
- `backend/src/main/java/com/bpdb/dms/service/ExpiryTrackingService.java`
- `frontend/src/pages/ExpiryTracking.tsx`

---

### 5. Stationery Tracking per Employee (FR-017)

**Status**: ⚠️ **PARTIALLY IMPLEMENTED**

**Requirement**:
- System shall track stationery and asset records per employee

**Current State**:
- `DocumentType.STATIONERY_RECORD` exists
- Asset management system exists (`Asset`, `AssetAssignment`)
- But no specific linking between stationery records and employees in document system

**Implementation Required**:
1. Add `assignedEmployee` field to `Document` entity (for STATIONERY_RECORD type)
2. Or create `StationeryRecord` entity extending/relating to Document
3. Add UI to assign stationery records to employees
4. Add reports showing stationery per employee
5. Link stationery records with asset assignments

**Files to Modify**:
- `backend/src/main/java/com/bpdb/dms/entity/Document.java` (add employee assignment)
- `backend/src/main/java/com/bpdb/dms/service/DocumentService.java` (add assignment logic)
- `frontend/src/pages/DocumentsEnhanced.tsx` (add employee assignment UI)

---

### 6. Document Archive and Restore (FR-036, FR-037, FR-090, FR-091)

**Status**: ⚠️ **PARTIALLY IMPLEMENTED**

**Requirement**:
- System shall provide archive functionality
- System shall support document restoration from archive
- System shall support recovery of deleted documents
- System shall support recovery of replaced documents

**Current State**:
- `DocumentVersion` has `isArchived` field
- Version archiving exists
- But no full document archiving/restoration
- No soft delete for documents (only `isActive` flag)

**Implementation Required**:
1. Add `isArchived` and `archivedAt` to `Document` entity
2. Add `deletedAt` for soft delete
3. Create archive/restore endpoints in `DocumentController`
4. Add archive management UI
5. Add restore functionality for deleted/replaced documents
6. Create archive reports

**Files to Modify**:
- `backend/src/main/java/com/bpdb/dms/entity/Document.java`
- `backend/src/main/java/com/bpdb/dms/service/DocumentService.java` (add archive/restore methods)
- `backend/src/main/java/com/bpdb/dms/controller/DocumentController.java`
- `frontend/src/pages/DocumentsEnhanced.tsx` (add archive UI)

---

### 7. Performance Security (PS) Renewal Form (FR-074, FR-075)

**Status**: ⚠️ **PARTIALLY IMPLEMENTED**

**Requirement**:
- System shall provide renewal update form for authorized users
- System shall allow extending PS coverage and uploading new documents

**Current State**:
- `ExpiryTracking` has `renewalDate` and `renewalDocumentId` fields
- But no dedicated renewal form/UI
- No workflow for PS renewal process

**Implementation Required**:
1. Create `PSRenewalRequest` entity/DTO
2. Create renewal form endpoint in `ExpiryTrackingController`
3. Add renewal form UI in frontend
4. Add document upload capability during renewal
5. Add approval workflow for renewals (if required)
6. Add grace period tracking and alerts

**Files to Create**:
- `backend/src/main/java/com/bpdb/dms/dto/PSRenewalRequest.java`
- `frontend/src/components/PSRenewalForm.tsx`

**Files to Modify**:
- `backend/src/main/java/com/bpdb/dms/controller/ExpiryTrackingController.java`
- `backend/src/main/java/com/bpdb/dms/service/ExpiryTrackingService.java`
- `frontend/src/pages/ExpiryTracking.tsx`

---

### 8. Folder Summary View (FR-039)

**Status**: ❌ **NOT IMPLEMENTED**

**Requirement**:
- System shall provide folder summary views:
  - Total files count
  - Uploaded files count
  - Remaining uploads

**Current State**:
- No folder system exists
- No summary calculations

**Implementation Required**:
1. Implement folder system (see #3 above)
2. Add `FolderSummary` DTO with:
   - `totalFiles`
   - `uploadedFiles`
   - `remainingUploads` (if quota system exists)
3. Create `getFolderSummary(Long folderId)` method
4. Add folder summary widget in UI

**Files to Create**:
- `backend/src/main/java/com/bpdb/dms/dto/FolderSummary.java`
- `frontend/src/components/FolderSummary.tsx`

---

### 9. Duplicate Detection (FR-015)

**Status**: ⚠️ **PARTIALLY IMPLEMENTED**

**Requirement**:
- System shall detect and handle duplicate documents

**Current State**:
- `Document` has `fileHash` field
- But no duplicate detection logic implemented
- No UI for handling duplicates

**Implementation Required**:
1. Implement duplicate detection in `FileUploadService`:
   - Check file hash before upload
   - Return duplicate warning if hash exists
2. Add duplicate handling options:
   - Skip upload
   - Upload as new version
   - Replace existing
3. Add duplicate detection UI
4. Add duplicate report/management page

**Files to Modify**:
- `backend/src/main/java/com/bpdb/dms/service/FileUploadService.java`
- `backend/src/main/java/com/bpdb/dms/controller/DocumentController.java`
- `frontend/src/components/BulkFileUpload.tsx`

---

### 10. Document Template Management (FR-027, FR-028, FR-029)

**Status**: ✅ **IMPLEMENTED** (but verify completeness)

**Requirement**:
- System shall support pre-defined templates per document type
- System shall allow customization of extraction templates
- System shall validate extracted data against templates

**Current State**:
- `DocumentTemplate` entity exists
- `DocumentTemplateService` exists
- `DocumentTemplateController` exists

**Verification Needed**:
- Check if template validation is fully implemented
- Check if template customization UI exists
- Verify template-based metadata extraction works

---

## Medium Priority Features

### 11. Tender Item Tracking (FR-018)

**Status**: ⚠️ **NEEDS VERIFICATION**

**Requirement**:
- System shall track tender items via OCR or manual entry

**Verification Needed**:
- Check if tender-specific metadata extraction exists
- Check if tender item tracking UI exists
- Verify OCR can extract tender numbers and items

---

### 12. Automatic Naming Convention (FR-014)

**Status**: ⚠️ **NEEDS VERIFICATION**

**Requirement**:
- System shall follow predefined naming conventions

**Verification Needed**:
- Check if automatic naming based on document type/metadata exists
- Check if naming convention is configurable

---

### 13. Document Attachment Linking (FR-016)

**Status**: ❌ **NOT IMPLEMENTED**

**Requirement**:
- System shall support document attachment linking

**Implementation Required**:
- Similar to document relationships, but for attachments
- Create attachment relationship system

---

## Summary

### Critical Priority (Must Implement)
1. ✅ DD1-DD4 Role-Based Upload System
2. ✅ Document Linking/Relationship System
3. ✅ Folder Structure and Organization
4. ✅ Dual Start Logic for Contracts
5. ✅ Document Archive and Restore

### High Priority (Should Implement)
6. ✅ Stationery Tracking per Employee
7. ✅ Performance Security Renewal Form
8. ✅ Folder Summary View
9. ✅ Duplicate Detection

### Medium Priority (Nice to Have)
10. ⚠️ Tender Item Tracking (verify)
11. ⚠️ Automatic Naming Convention (verify)
12. ⚠️ Document Attachment Linking

---

## Next Steps

1. **Prioritize** features based on business needs
2. **Create tickets** for each missing feature
3. **Estimate effort** for each implementation
4. **Plan sprints** to implement critical features
5. **Verify** partially implemented features for completeness

---

*Last Updated: [Current Date]*
*Analysis Based On: requirements.md, requirements-asset-management.md, draft_requirements.txt*

