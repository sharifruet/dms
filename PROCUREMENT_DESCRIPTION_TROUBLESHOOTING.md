# Procurement Description Extraction - Troubleshooting Guide

## Issue: Procurement Description Field is Blank

If the `procurementDescription` field is showing blank for newly uploaded TENDER_NOTICE documents, follow these steps:

## Step 1: Check Application Logs

After uploading a TENDER_NOTICE document, check the application logs for:

### Expected Success Logs:
```
INFO - Extracting metadata from database using regex patterns from document_type_fields for document: X (type: TENDER_NOTICE)
INFO - Processing TENDER_NOTICE document - will extract procurementDescription and other fields
INFO - Found X active fields for document type: TENDER_NOTICE
INFO - procurementDescription field found: true
INFO - Attempting to extract procurementDescription for document X (TENDER_NOTICE)
INFO - ✓ SUCCESS: Extracted procurement description for document X (length: Y chars)
INFO - Successfully extracted X fields for document Y: [procurementDescription, ...]
INFO - ✓ procurementDescription extracted for TENDER_NOTICE document X (length: Y chars)
```

### If Extraction Fails, You'll See:
```
WARN - ✗ FAILED: No procurement description extracted for document X. Check logs above for pattern matching details.
WARN - No procurement description pattern matched. Debugging information:
WARN - Found 'Package' at index X. Text around it...
```

## Step 2: Verify Field Configuration

Check if the field exists in the database:

```sql
SELECT field_key, field_label, is_ocr_mappable, ocr_pattern
FROM document_type_fields
WHERE document_type = 'TENDER_NOTICE'
  AND field_key = 'procurementDescription';
```

Expected result:
- `field_key`: `procurementDescription`
- `field_label`: `Procurement Description`
- `is_ocr_mappable`: `true`
- `ocr_pattern`: Should have a pattern (even though we use Java extraction)

## Step 3: Check Document Metadata

```sql
SELECT 
    d.id,
    d.document_type,
    d.file_name,
    CASE 
        WHEN d.extracted_text IS NULL THEN 'NO'
        WHEN d.extracted_text = '' THEN 'EMPTY'
        ELSE 'YES (' || LENGTH(d.extracted_text) || ' chars)'
    END as has_extracted_text,
    dm.key,
    LEFT(dm.value, 200) as value_preview,
    LENGTH(dm.value) as value_length
FROM documents d
LEFT JOIN document_metadata dm ON dm.document_id = d.id AND dm.key = 'procurementDescription'
WHERE d.document_type = 'TENDER_NOTICE'
ORDER BY d.id DESC
LIMIT 5;
```

## Step 4: Manual Extraction Test

Use the API endpoint to manually trigger extraction:

```bash
POST /api/documents/{documentId}/extract-metadata
```

This will:
1. Re-run the extraction
2. Return the extracted metadata
3. Log detailed debugging information

## Step 5: Check OCR Text Format

The extraction looks for patterns like:
- "Tender/Proposal GD-69 FY 25-26 Package No. and Description : 1) H-Type Connector..."
- "Package No. and Description : 1) H-Type Connector..."

If the OCR text format is different, the patterns might not match. Check the logs for the "Text around 'Package'" message to see the actual format.

## Common Issues and Solutions

### Issue 1: Field Not Configured
**Symptom**: Logs show "procurementDescription field found: false"

**Solution**: Run the database migration to ensure the field exists:
```sql
INSERT INTO document_type_fields (document_type, field_key, field_label, field_type, is_required, is_ocr_mappable, ocr_pattern, display_order, description)
VALUES 
('TENDER_NOTICE', 'procurementDescription', 'Procurement Description', 'text', false, true, 'Tender/Proposal\s+Package\s+No\.\s+and\s+Description\s*:\s*(GD-[0-9A-Za-z\- ]+?[0-9]{2}-[0-9]{2}.*?Nos\.)', 20, 'Tender/Proposal Package No. and Description')
ON CONFLICT (document_type, field_key) DO UPDATE 
SET is_ocr_mappable = true;
```

### Issue 2: Pattern Not Matching
**Symptom**: Logs show "No procurement description pattern matched"

**Solution**: 
1. Check the logs for the actual OCR text format
2. The extraction uses 5 different patterns with fallbacks
3. If none match, the OCR text format might be significantly different
4. Share the "Text around 'Package'" log output to adjust patterns

### Issue 3: Extraction Not Running
**Symptom**: No extraction logs at all

**Solution**: 
1. Verify `DatabaseMetadataExtractionService` is being called
2. Check that document type is `TENDER_NOTICE`
3. Verify `extracted_text` is not null/empty
4. Check for errors in logs

### Issue 4: Metadata Not Saving
**Symptom**: Extraction logs show success but metadata is still blank

**Solution**:
1. Check if `applyAutoMetadata` is being called
2. Verify no errors in `DocumentMetadataService`
3. Check database constraints on `document_metadata` table
4. Verify the document ID matches

## Debugging Commands

### Check Latest TENDER_NOTICE Document:
```sql
SELECT 
    id,
    document_type,
    file_name,
    LENGTH(extracted_text) as text_length,
    created_at
FROM documents
WHERE document_type = 'TENDER_NOTICE'
ORDER BY id DESC
LIMIT 1;
```

### Check Metadata for Latest Document:
```sql
SELECT 
    dm.key,
    LEFT(dm.value, 300) as value_preview,
    LENGTH(dm.value) as value_length
FROM document_metadata dm
JOIN documents d ON dm.document_id = d.id
WHERE d.document_type = 'TENDER_NOTICE'
  AND d.id = (SELECT MAX(id) FROM documents WHERE document_type = 'TENDER_NOTICE')
ORDER BY dm.key;
```

### Check Extraction Logs Pattern:
```bash
# In application logs, search for:
grep -i "procurementDescription\|procurement description" application.log | tail -20
```

## Next Steps

1. **Check logs** after uploading a new TENDER_NOTICE document
2. **Look for** the extraction messages listed above
3. **If extraction fails**, check the "Text around 'Package'" log output
4. **Share the log output** to help identify the OCR text format issue
5. **Use manual extraction** endpoint to test with existing documents

## Pattern Matching Details

The extraction uses 5 patterns in order:

1. **Pattern 1**: `Tender/Proposal ... Package No. and Description :`
2. **Pattern 2**: Handles line breaks between sections
3. **Pattern 3**: Just "Package No. and Description :" (if Tender/Proposal missing)
4. **Pattern 4**: Simple pattern with end detection
5. **Pattern 5**: Very flexible pattern as last resort

If all patterns fail, the logs will show detailed debugging information about what text was found around "Package" and "Tender" keywords.

