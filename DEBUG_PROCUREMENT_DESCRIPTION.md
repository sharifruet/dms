# Debugging Procurement Description Extraction

## Current Status
Procurement Description field is showing blank for newly uploaded TENDER_NOTICE documents.

## What Has Been Implemented

1. ✅ Java-based extraction method with 6 fallback patterns
2. ✅ Automatic extraction after OCR completion
3. ✅ Enhanced logging for debugging
4. ✅ Verification step to confirm metadata is saved
5. ✅ Diagnostic API endpoint

## Immediate Steps to Debug

### Step 1: Check Application Logs

After uploading a TENDER_NOTICE document, immediately check the logs for:

**Look for these log messages:**
```
INFO - Extracting metadata from database using regex patterns from document_type_fields for document: X (type: TENDER_NOTICE)
INFO - Processing TENDER_NOTICE document - will extract procurementDescription and other fields
INFO - Found X active fields for document type: TENDER_NOTICE
INFO - procurementDescription field found: true/false
INFO - Attempting to extract procurementDescription for document X (TENDER_NOTICE)
INFO - Keyword check - Package: true/false, Description: true/false, Tender/Proposal: true/false
```

**If extraction succeeds:**
```
INFO - ✓ SUCCESS: Extracted procurement description for document X (length: Y chars)
INFO - ✓✓ VERIFIED: procurementDescription extracted AND saved for TENDER_NOTICE document X
```

**If extraction fails:**
```
WARN - ✗ FAILED: No procurement description extracted for document X
ERROR - ❌ ALL PATTERNS FAILED: No procurement description pattern matched
ERROR - Found 'Package' at index X. Text around it: ...
```

### Step 2: Use Diagnostic API Endpoint

Call the diagnostic endpoint to see what's happening:

```bash
POST /api/documents/{documentId}/extract-metadata
```

**Response will include:**
- `metadata`: All extracted metadata
- `diagnostics.procurementDescriptionExtracted`: true/false
- `diagnostics.procurementDescriptionBefore`: Status before extraction
- `diagnostics.procurementDescriptionAfter`: Status after extraction
- `diagnostics.procurementDescriptionPreview`: First 300 chars if extracted

### Step 3: Check Database Configuration

Verify the field exists and is configured correctly:

```sql
SELECT 
    field_key,
    field_label,
    is_ocr_mappable,
    ocr_pattern IS NOT NULL as has_pattern,
    is_active
FROM document_type_fields
WHERE document_type = 'TENDER_NOTICE'
  AND field_key = 'procurementDescription';
```

**Expected:**
- `field_key`: `procurementDescription`
- `field_label`: `Procurement Description`
- `is_ocr_mappable`: `true`
- `has_pattern`: `true` (even though we use Java extraction)
- `is_active`: `true`

### Step 4: Check Document Metadata

```sql
SELECT 
    d.id,
    d.document_type,
    d.file_name,
    CASE 
        WHEN d.extracted_text IS NULL THEN 'NULL'
        WHEN d.extracted_text = '' THEN 'EMPTY'
        ELSE 'EXISTS (' || LENGTH(d.extracted_text) || ' chars)'
    END as extracted_text_status,
    dm.key,
    CASE 
        WHEN dm.value IS NULL THEN 'NULL'
        WHEN dm.value = '' THEN 'EMPTY'
        ELSE 'EXISTS (' || LENGTH(dm.value) || ' chars)'
    END as metadata_value_status,
    LEFT(dm.value, 200) as value_preview
FROM documents d
LEFT JOIN document_metadata dm ON dm.document_id = d.id AND dm.key = 'procurementDescription'
WHERE d.document_type = 'TENDER_NOTICE'
ORDER BY d.id DESC
LIMIT 5;
```

## Common Issues and Solutions

### Issue 1: Field Not Configured
**Symptom**: Logs show `procurementDescription field found: false`

**Solution**: Run this SQL:
```sql
INSERT INTO document_type_fields (document_type, field_key, field_label, field_type, is_required, is_ocr_mappable, ocr_pattern, display_order, description)
VALUES 
('TENDER_NOTICE', 'procurementDescription', 'Procurement Description', 'text', false, true, 'Tender/Proposal\s+Package\s+No\.\s+and\s+Description\s*:\s*(GD-[0-9A-Za-z\- ]+?[0-9]{2}-[0-9]{2}.*?Nos\.)', 20, 'Tender/Proposal Package No. and Description')
ON CONFLICT (document_type, field_key) DO UPDATE 
SET is_ocr_mappable = true,
    is_active = true;
```

### Issue 2: Extraction Not Running
**Symptom**: No extraction logs at all

**Check:**
1. Is `DatabaseMetadataExtractionService` being called?
2. Is document type `TENDER_NOTICE`?
3. Does document have `extracted_text`?
4. Check for errors in logs

### Issue 3: Patterns Not Matching
**Symptom**: Logs show "ALL PATTERNS FAILED"

**Solution**: 
1. Check the "Text around 'Package'" log output
2. The extraction uses 6 different patterns
3. If all fail, the OCR text format is significantly different
4. Share the log output to adjust patterns

### Issue 4: Extraction Succeeds But Not Saved
**Symptom**: Logs show extraction success but metadata is blank

**Check:**
- Look for "VERIFIED" message in logs
- If you see "ERROR: procurementDescription was extracted but NOT saved", there's a persistence issue
- Check for transaction rollbacks or errors in `DocumentMetadataService`

## What the Logs Will Tell You

The enhanced logging will show:

1. **If extraction is called**: Look for "Attempting to extract procurementDescription"
2. **If field is configured**: Look for "procurementDescription field found: true"
3. **If keywords exist**: Look for "Keyword check - Package: true/false"
4. **Which pattern matched**: Look for "Pattern X matched"
5. **If extraction succeeded**: Look for "✓ SUCCESS"
6. **If metadata was saved**: Look for "✓✓ VERIFIED"
7. **If all patterns failed**: Look for "❌ ALL PATTERNS FAILED" with debugging info

## Next Steps

1. **Upload a new TENDER_NOTICE document**
2. **Immediately check application logs** for the messages above
3. **Use the diagnostic endpoint** to see extraction status
4. **Share the log output** if extraction fails - especially the "Text around 'Package'" section
5. **Check database** to verify field configuration and metadata

The logs will now provide detailed information about why extraction is or isn't working.

