# Update: Using Database Regex Patterns for Metadata Extraction

## Summary

Updated `DatabaseMetadataExtractionService` to read regex patterns from the `document_type_fields` table instead of using hardcoded patterns.

## What Changed

### Before
- `DatabaseMetadataExtractionService` used hardcoded regex patterns
- Patterns were defined in Java code
- Only worked for TENDER_NOTICE documents

### After
- Reads regex patterns from `document_type_fields.ocr_pattern` column
- Uses PostgreSQL `regexp_matches()` function with patterns from database
- Works for all document types that have fields defined in the database

## Implementation Details

### Updated Method: `extractFieldsUsingDatabasePatterns()`

This method now:
1. Reads all active OCR-mappable fields from `document_type_fields` table for the document type
2. Uses PostgreSQL `regexp_matches()` function with each pattern
3. Extracts values and stores them in `document_metadata` table

### How It Works

```
After extracted_text is saved:
    ↓
DatabaseMetadataExtractionService.extractMetadataForDocument() called
    ↓
Reads document_type_fields for document type
    ↓
For each field with ocr_pattern:
    - Use PostgreSQL regexp_matches(extracted_text, ocr_pattern, 'i')
    - Extract captured value
    ↓
Save extracted values to document_metadata table
```

### Code Flow

1. **FileUploadService** (line 584):
   ```java
   databaseMetadataExtractionService.extractMetadataForDocument(documentId);
   ```

2. **DatabaseMetadataExtractionService.extractMetadataForDocument()**:
   - Gets document from database
   - Reads `document_type_fields` for document type
   - Calls `extractFieldsUsingDatabasePatterns()`

3. **extractFieldsUsingDatabasePatterns()**:
   - Queries `document_type_fields` table for active OCR-mappable fields
   - For each field with a pattern, calls `extractFieldUsingPostgreSQLRegex()`

4. **extractFieldUsingPostgreSQLRegex()**:
   - Executes: `SELECT (regexp_matches(extracted_text, pattern, 'i'))[1]`
   - Returns extracted value

5. **DocumentMetadataService.applyAutoMetadata()**:
   - Saves extracted values to `document_metadata` table

## Benefits

1. **Database-Driven**: Regex patterns stored in database can be updated without code changes
2. **PostgreSQL Native**: Uses optimized PostgreSQL regex engine
3. **Multi-Document Type**: Works for any document type with fields in database
4. **Consistent**: Same patterns used everywhere (DB extraction and DocumentTypeFieldService)

## Testing

### Verify Database Patterns Are Used

```sql
-- Check patterns in database
SELECT field_key, ocr_pattern 
FROM document_type_fields 
WHERE document_type = 'TENDER_NOTICE' 
AND is_ocr_mappable = true 
AND is_active = true;

-- Check if metadata was extracted
SELECT dm.key, dm.value 
FROM document_metadata dm
JOIN documents d ON d.id = dm.document_id
WHERE d.document_type = 'TENDER_NOTICE'
AND d.id = {your_document_id}
ORDER BY dm.key;
```

### Expected Behavior

When a document is uploaded:
1. OCR extracts text → saved to `documents.extracted_text`
2. `DatabaseMetadataExtractionService` reads patterns from `document_type_fields`
3. PostgreSQL regex extracts values using those patterns
4. Values saved to `document_metadata` table

## Status

✅ **Updated** - Service now reads patterns from database
✅ **Tested** - Code compiles without errors
✅ **Ready** - Will use database patterns on next document upload

The system now uses regex patterns from the `document_type_fields` table for all metadata extraction!

