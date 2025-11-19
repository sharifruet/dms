# End-to-End Testing Plan

## Overview
This document outlines the comprehensive end-to-end testing plan for the Document Management System (DMS) after implementing all TODO items.

## Test Coverage

### 1. Document Relationships (TODO #5)
**Status**: ✅ Implemented  
**Test Files**: `DocumentRelationshipsIntegrationTest.java`

**Test Scenarios**:
- ✅ Create relationship between documents
- ✅ Get relationships for a document
- ✅ Delete relationship
- ✅ Verify relationship types (CONTRACT_TO_BG, etc.)
- ✅ Test bidirectional relationship queries

**Manual Testing Checklist**:
- [ ] Open DocumentViewer
- [ ] Navigate to "Relationships" tab
- [ ] Click "Link Document"
- [ ] Select target document and relationship type
- [ ] Verify relationship appears in list
- [ ] Test unlinking a relationship
- [ ] Verify relationship is removed

---

### 2. Duplicate Detection (TODO #7)
**Status**: ✅ Implemented  
**Test Files**: `DuplicateDetectionIntegrationTest.java`

**Test Scenarios**:
- ✅ Detect duplicate file during upload
- ✅ Handle duplicate with "skip" action
- ✅ Handle duplicate with "version" action
- ✅ Handle duplicate with "replace" action
- ✅ Verify file hash calculation (SHA-256)
- ✅ Test duplicate detection with different file names but same content

**Manual Testing Checklist**:
- [ ] Upload a document
- [ ] Upload the same file again
- [ ] Verify duplicate dialog appears
- [ ] Test "Skip Upload" option
- [ ] Test "Upload as New Version" option
- [ ] Test "Replace Existing Document" option
- [ ] Verify duplicate detection works with renamed files

---

### 3. Document Archive and Restore (TODO #8)
**Status**: ✅ Implemented  
**Test Files**: `DocumentArchiveIntegrationTest.java`

**Test Scenarios**:
- ✅ Archive a document
- ✅ Restore archived document
- ✅ Soft delete a document
- ✅ Restore deleted document
- ✅ Get archived documents list
- ✅ Get deleted documents list
- ✅ Get archive statistics
- ✅ Batch archive operations
- ✅ Batch restore operations

**Manual Testing Checklist**:
- [ ] Navigate to Documents page
- [ ] Click archive icon on a document
- [ ] Navigate to Archive page
- [ ] Verify document appears in "Archived" tab
- [ ] Click restore button
- [ ] Verify document is restored
- [ ] Test soft delete functionality
- [ ] Test restore from deletion
- [ ] Verify statistics cards show correct counts

---

### 4. Stationery Tracking per Employee (TODO #9)
**Status**: ✅ Implemented  
**Test Files**: `StationeryTrackingIntegrationTest.java`

**Test Scenarios**:
- ✅ Assign stationery record to employee
- ✅ Unassign stationery from employee
- ✅ Get stationery records by employee
- ✅ Get all stationery records
- ✅ Get stationery statistics
- ✅ Get stationery statistics per employee
- ✅ Verify only STATIONERY_RECORD type can be assigned

**Manual Testing Checklist**:
- [ ] Upload a document with type "Stationery Record"
- [ ] Navigate to Stationery page
- [ ] Click assign button on a stationery record
- [ ] Select an employee from dropdown
- [ ] Verify assignment appears in table
- [ ] Test unassign functionality
- [ ] View "By Employee" tab
- [ ] Verify employee statistics show correct counts
- [ ] Verify statistics cards display correctly

---

### 5. DD1-DD4 Role Permissions (TODO #1-3)
**Status**: ✅ Implemented  
**Test Files**: `DDRolePermissionsIntegrationTest.java`

**Test Scenarios**:
- ✅ DD1 user can upload documents
- ✅ DD2 user can upload documents
- ✅ DD3 user can upload documents
- ✅ DD4 user can upload documents
- ✅ DD1-DD4 can reprocess OCR
- ✅ Verify role-based access control

**Manual Testing Checklist**:
- [ ] Login as DD1 user
- [ ] Verify upload button is visible
- [ ] Upload a document successfully
- [ ] Repeat for DD2, DD3, DD4 users
- [ ] Verify OCR reprocessing is accessible
- [ ] Test with different document types

---

### 6. Folder Integration (TODO #4)
**Status**: ✅ Implemented  
**Test Files**: Existing folder tests

**Test Scenarios**:
- ✅ Create folder
- ✅ Navigate folder hierarchy
- ✅ Filter documents by folder
- ✅ Move documents to folders
- ✅ View folder summary

**Manual Testing Checklist**:
- [ ] Create a new folder
- [ ] Create subfolders
- [ ] Upload document to folder
- [ ] Filter documents by folder
- [ ] Navigate folder tree
- [ ] Verify folder summary displays correctly

---

## Integration Test Execution

### Running All Integration Tests

```bash
# Backend integration tests
cd backend
./mvnw test -Dtest=*IntegrationTest

# Run specific test class
./mvnw test -Dtest=DocumentRelationshipsIntegrationTest

# Run with coverage
./mvnw test -Dtest=*IntegrationTest jacoco:report
```

### Test Environment Setup

1. **Database**: Uses in-memory H2 database for tests (configured in `application-test.properties`)
2. **Security**: Uses `@WithMockUser` for authentication
3. **Transactions**: All tests use `@Transactional` for rollback

---

## API Endpoint Testing

### Document Relationships
- `POST /api/documents/{id}/relationships` - Create relationship
- `GET /api/documents/{id}/relationships` - Get relationships
- `DELETE /api/documents/{id}/relationships/{relId}` - Delete relationship

### Duplicate Detection
- `POST /api/documents/upload` - Upload (returns duplicate info if found)
- `POST /api/documents/upload-duplicate` - Handle duplicate with action

### Archive and Restore
- `POST /api/documents/{id}/archive` - Archive document
- `POST /api/documents/{id}/restore-archive` - Restore archived
- `POST /api/documents/{id}/delete` - Soft delete
- `POST /api/documents/{id}/restore-delete` - Restore deleted
- `GET /api/documents/archived` - List archived
- `GET /api/documents/deleted` - List deleted
- `GET /api/documents/archive/statistics` - Get statistics

### Stationery Tracking
- `POST /api/documents/{id}/assign-stationery` - Assign to employee
- `POST /api/documents/{id}/unassign-stationery` - Unassign
- `GET /api/documents/stationery` - List all stationery
- `GET /api/documents/stationery/employee/{id}` - Get by employee
- `GET /api/documents/stationery/statistics` - Get statistics
- `GET /api/documents/stationery/statistics/employee` - Get per-employee stats

---

## Frontend Testing

### Component Testing
- DocumentViewer with Relationships tab
- Duplicate detection dialog
- Archive management page
- Stationery tracking page

### User Flow Testing
1. **Document Upload Flow**:
   - Upload document → Handle duplicate → Assign to folder → View in list

2. **Archive Flow**:
   - View document → Archive → View in archive → Restore

3. **Stationery Flow**:
   - Upload stationery record → Assign to employee → View statistics

4. **Relationship Flow**:
   - View document → Add relationship → View relationships → Remove relationship

---

## Performance Testing

### Load Testing
- Upload 100 documents simultaneously
- Query relationships for document with 50+ relationships
- Archive/unarchive operations under load
- Stationery statistics calculation with 1000+ records

### Stress Testing
- Maximum concurrent uploads
- Large file uploads (100MB+)
- Complex relationship graphs
- Bulk archive operations

---

## Security Testing

### Authentication & Authorization
- ✅ Verify DD1-DD4 roles have correct permissions
- ✅ Test unauthorized access attempts
- ✅ Verify JWT token validation
- ✅ Test role-based UI visibility

### Data Validation
- ✅ File type validation
- ✅ File size limits
- ✅ SQL injection prevention
- ✅ XSS prevention in document metadata

---

## Test Results Tracking

### Test Execution Log
- Date: [Date]
- Tester: [Name]
- Environment: [Local/Dev/Staging]
- Results: [Pass/Fail/Partial]

### Known Issues
1. [Issue description]
2. [Issue description]

### Test Coverage Metrics
- Unit Tests: [X]%
- Integration Tests: [X]%
- E2E Tests: [X]%
- Total Coverage: [X]%

---

## Next Steps

1. ✅ Complete all integration tests
2. ⚠️ Set up CI/CD pipeline for automated testing
3. ⚠️ Add performance benchmarks
4. ⚠️ Create test data fixtures
5. ⚠️ Document test scenarios in test management tool

---

*Last Updated: [Date]*  
*Test Plan Version: 1.0*

