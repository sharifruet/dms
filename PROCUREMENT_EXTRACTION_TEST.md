# Procurement Description Extraction - Testing Guide

## Summary

The procurement description extraction has been implemented with Java-based extraction (instead of PostgreSQL regex) to handle the complex pattern matching required.

## What Was Implemented

1. **Java-based extraction method** (`extractProcurementDescription`) in `DatabaseMetadataExtractionService`
   - Uses multiple regex patterns with fallbacks
   - Handles variations in OCR text formatting
   - Removes "Tender/Proposal", "Package No. and", and "Description :" labels

2. **Enhanced logging** to help debug extraction issues
   - Logs when extraction starts
   - Shows which pattern matched
   - Displays extraction results

3. **New API endpoint** for manual testing:
   - `POST /api/documents/{id}/extract-metadata`

## How to Test

### Option 1: Check Application Logs

After uploading a tender notice document, check the application logs for:

```
INFO  - Extracting metadata from database using regex patterns...
INFO  - Found X active fields for document type: TENDER_NOTICE
INFO  - procurementDescription field found: true
INFO  - Extracted procurement description for document X (length: Y chars)
INFO  - Successfully extracted X fields for document Y: [procurementDescription, ...]
```

### Option 2: Use the API Endpoint

```bash
# Get document ID first
curl -X GET "http://localhost:8080/api/documents?size=1"

# Trigger extraction (requires authentication)
curl -X POST "http://localhost:8080/api/documents/{documentId}/extract-metadata" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

### Option 3: Check Database Directly

Query the `document_metadata` table:

```sql
SELECT dm.key, dm.value 
FROM document_metadata dm
JOIN documents d ON dm.document_id = d.id
WHERE d.document_type = 'TENDER_NOTICE'
  AND dm.key = 'procurementDescription';
```

### Option 4: Use the Test Script

```bash
cd backend
./test-extraction.sh [document_id]
```

## Expected Result

The `procurementDescription` metadata field should contain:

```
GD-69 FY 25-26

 1) H-Type Connector (Dog to Dog) 4?? 32,500 Nos. 2) H-Type Connector
 (WASP to WASP) 4?? 38,800 Nos. 3) H-Type Connector (ANT to ANT) a??
12,600 Nos. 4) H-Type Connector (Dog to 35 rm PVC Copper Cable)a??
6,862 Nos. 5) Terminal lug 35 rm a?? 27,446 Nos. 6) 33 kV PG Clamp a??
10,708 Nos.
```

**Note:** The labels "Tender/Proposal", "Package No. and", and "Description :" should be removed.

## Troubleshooting

If extraction fails:

1. **Check document type**: Must be `TENDER_NOTICE`
2. **Check extracted text**: Document must have `extracted_text` populated
3. **Check field configuration**: Verify `procurementDescription` exists in `document_type_fields` table
4. **Check logs**: Look for pattern matching failures or errors
5. **Check OCR text format**: The pattern looks for "Tender/Proposal" followed by "Package No. and Description :"

## Pattern Matching

The extraction uses 4 fallback patterns:

1. **Pattern 1**: `Tender/Proposal ... Package No. and Description :`
2. **Pattern 2**: Handles line breaks between sections
3. **Pattern 3**: Just "Package No. and Description :" (if "Tender/Proposal" is missing)
4. **Pattern 4**: More flexible pattern with better end detection

If none match, check the logs for the text sample around "Package" to see the actual format.

