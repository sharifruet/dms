# Frontend OCR Text Display Fix

## Problem

The OCR extracted text was stored in the database (`documents.extracted_text`), but it wasn't showing in the frontend when viewing a document.

## Root Cause

1. **`@JsonIgnore` on Document Entity**: The `extractedText` field in the `Document` entity has `@JsonIgnore` annotation, which prevents it from being serialized in JSON responses.

2. **Backend Only Checked Elasticsearch**: The `/api/documents/{id}` endpoint was only retrieving OCR text from Elasticsearch, not from the database's `extracted_text` field.

3. **Frontend Expectation**: The frontend was looking for `documentData.ocrText`, which was empty when Elasticsearch didn't have the data.

## Solution

Updated the backend `DocumentController.getDocument()` method to:

1. **Prioritize Database**: First check `documents.extracted_text` field in the database (primary source)
2. **Fallback to Elasticsearch**: If database doesn't have text, fallback to Elasticsearch
3. **Set Response Fields**: Explicitly set `ocrText` in the response so frontend can read it

## Changes Made

**File**: `backend/src/main/java/com/bpdb/dms/controller/DocumentController.java`

### Key Changes:

1. **Read from Database First** (Line 124-130):
   ```java
   String dbExtractedText = document.getExtractedText();
   if (dbExtractedText != null && !dbExtractedText.trim().isEmpty()) {
       ocrText = dbExtractedText;
       response.put("ocrProcessing", false);
   }
   ```

2. **Fallback to Elasticsearch** (Line 132-169):
   - Only if database doesn't have text
   - Handles errors gracefully

3. **Set Response Fields** (Line 172-177):
   ```java
   response.put("ocrText", ocrText != null ? ocrText : "");
   response.put("ocrConfidence", ocrConfidence);
   response.put("extractedText", dbExtractedText != null ? dbExtractedText : "");
   ```

4. **Added Logger**: Added logger import and field for debugging

## Frontend Behavior

The frontend `DocumentViewer.tsx` component checks for OCR text in this order:

1. `documentData.ocrText` ✅ **NOW POPULATED**
2. `documentData.document.extractedText` (won't work due to @JsonIgnore)
3. `documentData.document.ocrText`
4. Falls back to separate OCR endpoint call

Since we're now setting `ocrText` in the response from the database, the frontend will pick it up on the first check.

## How It Works Now

```
User views document in frontend
    ↓
Frontend calls GET /api/documents/{id}
    ↓
Backend retrieves Document from database
    ↓
Backend checks documents.extracted_text field
    │
    ├─ Has text → Use database value ✅
    │
    └─ No text → Check Elasticsearch
        │
        ├─ Has text → Use Elasticsearch value
        │
        └─ No text → Return empty
    ↓
Backend sets ocrText in response
    ↓
Frontend receives ocrText and displays it ✅
```

## Testing

1. **Upload a PDF or image document**
2. **Wait for OCR processing** (check logs)
3. **View the document in frontend**
4. **Verify OCR text is displayed** in the OCR tab/section

## Verification

To verify the fix is working:

1. **Check Database**:
   ```sql
   SELECT id, file_name, 
          LENGTH(extracted_text) as text_length
   FROM documents
   WHERE id = {your_document_id};
   ```

2. **Check API Response**:
   ```bash
   curl -H "Authorization: Bearer {token}" \
        http://localhost:8080/api/documents/{id}
   ```
   Should include:
   ```json
   {
     "ocrText": "extracted text here...",
     "extractedText": "extracted text here...",
     "ocrProcessing": false
   }
   ```

3. **Check Frontend**: View document and verify OCR text appears

## Status

✅ **Fixed** - Backend now reads `extractedText` from database
✅ **Fixed** - Response includes `ocrText` field for frontend
✅ **Working** - Frontend should now display OCR text correctly

The OCR text from `documents.extracted_text` should now be visible in the frontend when viewing documents.

