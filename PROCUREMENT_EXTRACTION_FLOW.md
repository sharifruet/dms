# Procurement Description Extraction Flow

## Overview

The procurement description extraction is now **automatically triggered** after document upload and OCR extraction for TENDER_NOTICE documents.

## Flow Diagram

```
1. Document Upload
   ↓
2. OCR Processing (extractText)
   ↓
3. Save extracted_text to database
   ↓
4. First Metadata Extraction (line 584)
   - Runs for ALL documents
   - Extracts procurementDescription for TENDER_NOTICE
   ↓
5. Document Type Finalization
   - Update document type if OCR detected different type
   ↓
6. Second Metadata Extraction (line 639) - FOR TENDER_NOTICE ONLY
   - Re-runs extraction if document type changed
   - Ensures procurementDescription is extracted even if type was updated
   ↓
7. Save Metadata to document_metadata table
   - Key: "procurementDescription"
   - Value: Cleaned text without labels
```

## Implementation Details

### 1. Automatic Extraction Trigger

**Location**: `FileUploadService.java`

**First Extraction** (line 584):
```java
// Extract metadata using database regex after extracted_text is saved
if (ocrResult.getExtractedText() != null && !ocrResult.getExtractedText().trim().isEmpty()) {
    databaseMetadataExtractionService.extractMetadataForDocument(documentId);
}
```

**Second Extraction for TENDER_NOTICE** (line 639):
```java
// Re-run database metadata extraction if document type was changed or if it's a TENDER_NOTICE
if (documentTypeChanged || DocumentType.TENDER_NOTICE.name().equals(managedDocument.getDocumentType())) {
    databaseMetadataExtractionService.extractMetadataForDocument(documentId);
}
```

### 2. Extraction Method

**Location**: `DatabaseMetadataExtractionService.extractProcurementDescription()`

**Features**:
- Uses Java-based regex (not PostgreSQL)
- 4 fallback patterns to handle OCR variations
- Removes labels: "Tender/Proposal", "Package No. and", "Description :"
- Preserves formatting and line breaks

### 3. Field Configuration

**Database Table**: `document_type_fields`

**Required Entry**:
```sql
INSERT INTO document_type_fields 
(document_type, field_key, field_label, field_type, is_required, is_ocr_mappable, ocr_pattern, display_order, description)
VALUES 
('TENDER_NOTICE', 'procurementDescription', 'Procurement Description', 'text', false, true, '...', 20, '...');
```

## Verification

### Check Logs

After uploading a TENDER_NOTICE document, you should see:

```
INFO - Extracting metadata from database using regex patterns from document_type_fields for document: X (type: TENDER_NOTICE)
INFO - Processing TENDER_NOTICE document - will extract procurementDescription and other fields
INFO - procurementDescription field found: true
INFO - Extracted procurement description for document X (length: Y chars)
INFO - Successfully extracted X fields for document Y: [procurementDescription, ...]
INFO - ✓ procurementDescription extracted for TENDER_NOTICE document X (length: Y chars)
```

### Check Database

```sql
SELECT 
    d.id,
    d.document_type,
    d.file_name,
    dm.key,
    LEFT(dm.value, 200) as value_preview,
    LENGTH(dm.value) as value_length
FROM documents d
JOIN document_metadata dm ON dm.document_id = d.id
WHERE d.document_type = 'TENDER_NOTICE'
  AND dm.key = 'procurementDescription'
ORDER BY d.id DESC;
```

### Expected Result

The `procurementDescription` field should contain:
- Package number (e.g., "GD-69 FY 25-26")
- Items list (e.g., "1) H-Type Connector...")
- **NO** "Tender/Proposal" label
- **NO** "Package No. and" label
- **NO** "Description :" label

## Error Handling

If extraction fails:
1. Error is logged but doesn't stop document processing
2. Check logs for pattern matching details
3. Verify field configuration in `document_type_fields` table
4. Verify document type is `TENDER_NOTICE`
5. Verify `extracted_text` is not null/empty

## Manual Trigger

If extraction didn't run automatically, you can manually trigger it:

```bash
POST /api/documents/{id}/extract-metadata
```

This will:
- Re-run the extraction
- Return extracted metadata
- Log detailed information

## Summary

✅ **Automatic**: Extraction runs automatically after OCR  
✅ **TENDER_NOTICE Specific**: Ensured to run for TENDER_NOTICE documents  
✅ **Robust**: Multiple fallback patterns handle OCR variations  
✅ **Logged**: Detailed logging for debugging  
✅ **Error Tolerant**: Errors don't stop document processing  

