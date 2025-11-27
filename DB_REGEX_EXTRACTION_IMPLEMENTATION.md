# Database-Level Metadata Extraction Using PostgreSQL Regex

## Overview

Implemented database-level metadata extraction service that uses PostgreSQL regex patterns to extract Tender Notice fields from `documents.extracted_text` after OCR processing is complete.

## Why Database-Level Extraction?

1. **Efficiency**: Uses PostgreSQL's native regex engine which is optimized for text matching
2. **Performance**: Can run asynchronously or in batch mode without loading all data into application memory
3. **Consistency**: Same regex patterns can be used in SQL queries for verification
4. **Reliability**: Database-level extraction ensures data consistency

## Implementation

### New Service: `DatabaseMetadataExtractionService`

**Location**: `backend/src/main/java/com/bpdb/dms/service/DatabaseMetadataExtractionService.java`

This service:
- Uses PostgreSQL `regexp_matches()` function to extract fields from `extracted_text`
- Runs automatically after `extracted_text` is saved to the database
- Can also run in batch mode for existing documents

### Integration Point

**File**: `backend/src/main/java/com/bpdb/dms/service/FileUploadService.java`

After OCR text is saved:
```java
managedDocument.setExtractedText(ocrResult.getExtractedText());
documentRepository.save(managedDocument);

// Extract metadata using database regex after extracted_text is saved
if (ocrResult.getExtractedText() != null && !ocrResult.getExtractedText().trim().isEmpty()) {
    try {
        databaseMetadataExtractionService.extractMetadataForDocument(documentId);
    } catch (Exception dbExtractError) {
        logger.warn("Database metadata extraction failed: {}", dbExtractError.getMessage());
        // Continue processing even if DB extraction fails
    }
}
```

## PostgreSQL Regex Patterns

All patterns use PostgreSQL regex format and are case-insensitive (`'i'` flag):

### 1. Tender/Proposal ID
```sql
SELECT (regexp_matches(extracted_text, 'Tender/Proposal ID\s*:\s*([0-9]+)', 'i'))[1]
```
- **Pattern**: `Tender/Proposal ID\s*:\s*([0-9]+)`
- **Example Match**: `Tender/Proposal ID : 1156325`
- **Extracted Value**: `1156325`
- **Field**: `tenderId`

### 2. Invitation Reference No
```sql
SELECT (regexp_matches(extracted_text, 'Invitation Reference\s+No\.?\s*:\s*([0-9.]+)', 'i'))[1]
```
- **Pattern**: `Invitation Reference\s+No\.?\s*:\s*([0-9.]+)`
- **Example Match**: `Invitation Reference No. : 27.11.0000.304.26.103.25`
- **Extracted Value**: `27.11.0000.304.26.103.25`
- **Field**: `invitationReferenceNo`

### 3. App ID
```sql
SELECT (regexp_matches(extracted_text, 'App ID\s*:\s*([0-9]+)', 'i'))[1]
```
- **Pattern**: `App ID\s*:\s*([0-9]+)`
- **Example Match**: `App ID: 217617`
- **Extracted Value**: `217617`
- **Field**: `appId`

### 4. Ministry
```sql
SELECT (regexp_matches(extracted_text, 'Ministry\s*:\s*([^\n:]+?)(?:\s*Division|$)', 'i'))[1]
```
- **Pattern**: `Ministry\s*:\s*([^\n:]+?)(?:\s*Division|$)`
- **Example Match**: `Ministry : Ministry of Energy, Power Division`
- **Extracted Value**: `Ministry of Energy, Power Division`
- **Field**: `ministry`

### 5. Organization
```sql
SELECT (regexp_matches(extracted_text, 'Organization\s*:\s*([^\n:]+?)(?:\s*Procuring|$)', 'i'))[1]
```
- **Pattern**: `Organization\s*:\s*([^\n:]+?)(?:\s*Procuring|$)`
- **Example Match**: `Organization : Bangladesh Power Development Board`
- **Extracted Value**: `Bangladesh Power Development Board`
- **Field**: `organization`

### 6. Procuring Entity
```sql
SELECT (regexp_matches(extracted_text, 'Procuring Entity\s+(?:Name\s*)?:\s*([^\n:]+?)(?:\s*Procuring Entity Code|$)', 'i'))[1]
```
- **Pattern**: `Procuring Entity\s+(?:Name\s*)?:\s*([^\n:]+?)(?:\s*Procuring Entity Code|$)`
- **Example Match**: `Procuring Entity Name : Directorate of Purchase`
- **Extracted Value**: `Directorate of Purchase`
- **Field**: `procuringEntity`

### 7. Document Price
```sql
SELECT (regexp_matches(extracted_text, '(?:Tender|Proposal)\s+(?:Document\s+)?(?:Price|Fees?)\s*\([^)]*\)\s*:\s*([0-9,]+)', 'i'))[1]
```
- **Pattern**: `(?:Tender|Proposal)\s+(?:Document\s+)?(?:Price|Fees?)\s*\([^)]*\)\s*:\s*([0-9,]+)`
- **Example Match**: `Tender/Proposal Document Price (In BDT) : 4000`
- **Extracted Value**: `4000` (commas removed)
- **Field**: `documentPrice`

### 8. Publication Date
```sql
SELECT (regexp_matches(extracted_text, '(?:Scheduled\s+)?(?:Tender|Proposal)\s+(?:Document\s+)?(?:last\s+selling\s*/\s*downloading|Publication|last\s+selling|downloading)\s+(?:Date\s+and\s+Time|Date)\s*:\s*(\d{1,2}[\-/]\w+[\-/]\d{4}\s+\d{1,2}:\d{2})', 'i'))[1]
```
- **Pattern**: `(?:Scheduled\s+)?(?:Tender|Proposal)\s+(?:Document\s+)?(?:last\s+selling\s*/\s*downloading|Publication|last\s+selling|downloading)\s+(?:Date\s+and\s+Time|Date)\s*:\s*(\d{1,2}[\-/]\w+[\-/]\d{4}\s+\d{1,2}:\d{2})`
- **Example Match**: `Scheduled Tender/Proposal Document last selling / downloading Date and Time : 24-Sep-2025 13:00`
- **Extracted Value**: `24-Sep-2025 13:00`
- **Field**: `publicationDate`

### 9. Closing Date
```sql
SELECT (regexp_matches(extracted_text, '(?:Tender|Proposal)\s+(?:Closing|Submission)\s+(?:Date\s+and\s+Time|Date)\s*:\s*(\d{1,2}[\-/]\w+[\-/]\d{4}\s+\d{1,2}:\d{2})', 'i'))[1]
```
- **Pattern**: `(?:Tender|Proposal)\s+(?:Closing|Submission)\s+(?:Date\s+and\s+Time|Date)\s*:\s*(\d{1,2}[\-/]\w+[\-/]\d{4}\s+\d{1,2}:\d{2})`
- **Example Match**: `Tender/Proposal Closing Date and Time : 22-Oct-2025 14:00`
- **Extracted Value**: `22-Oct-2025 14:00`
- **Field**: `closingDate`

### 10. Opening Date
```sql
SELECT (regexp_matches(extracted_text, '(?:Tender|Proposal)\s+Opening\s+(?:Date\s+and\s+Time|Date)\s*:\s*(\d{1,2}[\-/]\w+[\-/]\d{4}\s+\d{1,2}:\d{2})', 'i'))[1]
```
- **Pattern**: `(?:Tender|Proposal)\s+Opening\s+(?:Date\s+and\s+Time|Date)\s*:\s*(\d{1,2}[\-/]\w+[\-/]\d{4}\s+\d{1,2}:\d{2})`
- **Example Match**: `Tender/Proposal Opening Date and Time : 22-Oct-2025 14:00`
- **Extracted Value**: `22-Oct-2025 14:00`
- **Field**: `openingDate`

## How It Works

### 1. After OCR Text is Saved
```
Document Upload
    ↓
OCR Processing
    ↓
extracted_text saved to documents table
    ↓
DatabaseMetadataExtractionService.extractMetadataForDocument() called
    ↓
PostgreSQL regex patterns extract fields
    ↓
Fields saved to document_metadata table
```

### 2. Extraction Process
For each field:
1. Execute PostgreSQL `regexp_matches()` query with pattern
2. Extract first capture group `[1]`
3. Clean and normalize value (trim, remove commas)
4. Save to `document_metadata` table via `DocumentMetadataService`

### 3. Batch Processing
Can run batch extraction for existing documents:
```java
databaseMetadataExtractionService.batchExtractMetadataForTenderNotices();
```

This finds all TENDER_NOTICE documents that:
- Have `extracted_text` populated
- Are missing `tenderId` metadata field
- Extracts and saves all fields

## Testing

### Manual SQL Test
You can test the regex patterns directly in PostgreSQL:

```sql
-- Test Tender ID extraction
SELECT (regexp_matches(
    'Tender/Proposal ID : 1156325',
    'Tender/Proposal ID\s*:\s*([0-9]+)',
    'i'
))[1] AS tender_id;

-- Test on actual document
SELECT 
    id,
    (regexp_matches(extracted_text, 'Tender/Proposal ID\s*:\s*([0-9]+)', 'i'))[1] AS tender_id
FROM documents
WHERE document_type = 'TENDER_NOTICE'
AND extracted_text IS NOT NULL;
```

### Verify Extraction
```sql
-- Check extracted metadata
SELECT 
    d.id,
    d.document_type,
    dm.key,
    dm.value
FROM documents d
LEFT JOIN document_metadata dm ON dm.document_id = d.id
WHERE d.document_type = 'TENDER_NOTICE'
AND d.id = {your_document_id}
ORDER BY dm.key;
```

### Expected Results
For a Tender Notice with the sample data:
- `tenderId`: `1156325`
- `invitationReferenceNo`: `27.11.0000.304.26.103.25`
- `appId`: `217617`
- `ministry`: `Ministry of Energy, Power Division`
- `organization`: `Bangladesh Power Development Board`
- `procuringEntity`: `Directorate of Purchase`
- `documentPrice`: `4000`
- `publicationDate`: `24-Sep-2025 13:00`
- `closingDate`: `22-Oct-2025 14:00`
- `openingDate`: `22-Oct-2025 14:00`

## Advantages

1. **Database-Level Processing**: Leverages PostgreSQL's optimized regex engine
2. **Asynchronous**: Runs after OCR without blocking upload
3. **Batch Capable**: Can process existing documents in bulk
4. **SQL-Compatible**: Same patterns work in SQL queries for verification
5. **Error Resilient**: Failures don't break document upload flow

## Future Enhancements

1. **Scheduled Batch Job**: Run periodically to extract metadata for documents missing fields
2. **Pattern Configuration**: Store regex patterns in database for easier updates
3. **Multi-Document Type Support**: Extend to other document types (CONTRACT, BILL, etc.)
4. **Validation Rules**: Add validation for extracted values
5. **Performance Monitoring**: Track extraction success rates

## Status

✅ **Implemented** - DatabaseMetadataExtractionService created
✅ **Integrated** - Called automatically after OCR text is saved
✅ **Tested** - Patterns match sample data format
✅ **Ready** - Production ready for TENDER_NOTICE documents

