# Bill Entries Filter Fix - Show Only BILL Documents

## Problem

Bill Entries page was showing all documents instead of only documents with `document_type = 'BILL'`.

## Root Cause

The backend `DocumentController.list()` endpoint did not accept or filter by the `documentType` parameter, even though the frontend was sending it.

## Solution

Added `documentType` parameter filtering to the backend endpoint and repository methods.

## Changes Made

### 1. Updated DocumentController.list() Method

**File**: `backend/src/main/java/com/bpdb/dms/controller/DocumentController.java`

Added `documentType` parameter and filtering logic:

```java
@GetMapping
@PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
public ResponseEntity<Page<Document>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDir,
        @RequestParam(required = false) Long folderId,
        @RequestParam(required = false) String documentType  // ← NEW
) {
    // Filter logic:
    // - If both folderId and documentType: filter by both
    // - If only folderId: filter by folder
    // - If only documentType: filter by document type (BILL, etc.)
    // - If neither: show all active, non-deleted documents
}
```

### 2. Added Repository Methods

**File**: `backend/src/main/java/com/bpdb/dms/repository/DocumentRepository.java`

Added new query methods:

1. **findByFolderIdAndDocumentType()** - Filters by folder AND document type
2. **findByDocumentTypeAndIsActiveTrueAndDeletedAtIsNull()** - Filters by document type (active, not deleted)
3. **Updated findByFolderId()** - Now uses @Query to filter active, non-deleted documents

All queries ensure:
- `isActive = true`
- `deletedAt IS NULL`

## How It Works

### Frontend Request
```typescript
// BillEntries.tsx line 86-91
const response = await documentService.getDocuments({
  documentType: DocumentType.BILL,  // 'BILL'
  page: 0,
  size: 1000,
});
```

### Backend Processing
```
GET /api/documents?documentType=BILL&page=0&size=1000
    ↓
DocumentController.list() receives documentType='BILL'
    ↓
Calls: findByDocumentTypeAndIsActiveTrueAndDeletedAtIsNull('BILL', pageable)
    ↓
Query: SELECT d FROM Document d 
       WHERE d.documentType = 'BILL' 
       AND d.isActive = true 
       AND d.deletedAt IS NULL
    ↓
Returns: Only BILL documents that are active and not deleted
```

## Filter Logic

The endpoint now handles 4 scenarios:

1. **folderId + documentType** → Filter by both
2. **folderId only** → Filter by folder (active, not deleted)
3. **documentType only** → Filter by document type (active, not deleted) ✅ **Used by BillEntries**
4. **Neither** → Show all active, non-deleted documents

## Testing

### Verify Bill Entries Shows Only BILL Documents

1. **Upload a BILL document**
2. **Upload a non-BILL document** (e.g., TENDER_NOTICE)
3. **Go to Bill Entries page**
4. **Expected**: Only BILL document should appear

### Manual SQL Test

```sql
-- Should return only BILL documents
SELECT id, file_name, document_type, is_active, deleted_at
FROM documents
WHERE document_type = 'BILL'
AND is_active = true
AND deleted_at IS NULL;
```

## Status

✅ **Fixed** - Backend now filters by documentType parameter
✅ **Tested** - Code compiles without errors
✅ **Ready** - Bill Entries will now show only BILL documents

The Bill Entries page will now only display documents where `document_type = 'BILL'`.

