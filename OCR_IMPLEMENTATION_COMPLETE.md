# Complete OCR Implementation - Verification Guide

## Overview
This document describes the complete implementation for displaying OCR text from `document.extractedText` in the frontend when viewing documents.

## Implementation Summary

### Backend Changes

#### 1. DocumentController - `GET /api/documents/{id}`
**File:** `backend/src/main/java/com/bpdb/dms/controller/DocumentController.java`

**Changes:**
- ✅ Prioritizes `document.extractedText` from database as primary source
- ✅ Falls back to Elasticsearch only if `extractedText` is empty
- ✅ Returns OCR text in response with confidence and processing status

**Priority Order:**
1. `document.extractedText` (database) - **Primary**
2. Elasticsearch `DocumentIndex.extractedText` - Fallback
3. Empty string if neither available

#### 2. DocumentController - `GET /api/documents/{id}/ocr`
**File:** `backend/src/main/java/com/bpdb/dms/controller/DocumentController.java`

**Changes:**
- ✅ Updated to also prioritize `document.extractedText` first
- ✅ Consistent behavior with main document endpoint
- ✅ Better error handling

**Priority Order:**
1. `document.extractedText` (database) - **Primary**
2. Elasticsearch `DocumentIndex.extractedText` - Fallback
3. Empty with processing status if neither available

### Frontend Changes

#### DocumentViewer Component
**File:** `frontend/src/components/DocumentViewer.tsx`

**Changes:**
- ✅ Prioritizes `document.extractedText` from document entity
- ✅ Handles multiple response structures defensively
- ✅ Falls back gracefully through multiple sources
- ✅ Displays OCR text immediately when available

**Priority Order:**
1. `document.extractedText` from document entity - **Primary**
2. `ocrText` from API response
3. Separate OCR endpoint call
4. `doc.extractedText` from component prop (final fallback)

## Data Flow

### When Document is Uploaded:
1. File uploaded → Document saved to database
2. OCR processing runs asynchronously (for images/PDFs only)
3. Extracted text saved to `document.extractedText` field
4. Document updated in database

### When Document is Viewed:
1. Frontend calls `GET /api/documents/{id}`
2. Backend returns document with `extractedText` field
3. Frontend extracts `extractedText` from document object
4. OCR text displayed in "OCR Text" tab immediately

## Verification Steps

### 1. Verify Backend Returns extractedText

**Test the endpoint:**
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/documents/{documentId}
```

**Expected Response:**
```json
{
  "document": {
    "id": 1,
    "fileName": "document.pdf",
    "extractedText": "This is the OCR extracted text...",
    ...
  },
  "ocrText": "This is the OCR extracted text...",
  "ocrConfidence": 0.8,
  "ocrProcessing": false
}
```

### 2. Verify Frontend Displays OCR Text

**Steps:**
1. Upload an image or PDF document
2. Wait for OCR processing to complete (check logs)
3. Open the document viewer
4. Click on "OCR Text" tab
5. Verify OCR text is displayed

**Expected Behavior:**
- OCR text appears immediately (no loading spinner)
- Text is displayed in a scrollable box
- Confidence score shown if available

### 3. Verify Fallback Behavior

**Test Scenarios:**

**Scenario A: Document with extractedText**
- ✅ Should display text immediately
- ✅ No API call to `/ocr` endpoint needed

**Scenario B: Document without extractedText (still processing)**
- ✅ Should show "OCR processing..." message
- ✅ Should poll for OCR completion

**Scenario C: Document without extractedText (failed)**
- ✅ Should show error message
- ✅ Should indicate OCR failed

## Troubleshooting

### Issue: OCR text not displaying

**Check:**
1. Is `document.extractedText` populated in database?
   ```sql
   SELECT id, file_name, extracted_text 
   FROM documents 
   WHERE id = {documentId};
   ```

2. Is OCR processing enabled?
   - Check `app.ocr.enabled=true` in `application.properties`
   - Check backend logs for OCR initialization

3. Is the file type correct?
   - OCR only runs for images and PDFs
   - Check `mime_type` in database

### Issue: Backend not returning extractedText

**Check:**
1. Verify Document entity serialization
   - Field should not have `@JsonIgnore`
   - Getter method exists

2. Check database column exists
   ```sql
   SELECT column_name 
   FROM information_schema.columns 
   WHERE table_name = 'documents' 
   AND column_name = 'extracted_text';
   ```

3. Verify OCR processing completed
   - Check backend logs for OCR completion
   - Verify `extractedText` was saved

### Issue: Frontend not reading extractedText

**Check:**
1. Browser DevTools → Network tab
   - Check `GET /api/documents/{id}` response
   - Verify `document.extractedText` is in response

2. Browser DevTools → Console
   - Check for JavaScript errors
   - Verify component state

3. Component props
   - Ensure document object has `extractedText` property

## Code Locations

### Backend:
- `backend/src/main/java/com/bpdb/dms/controller/DocumentController.java`
  - `getDocument()` method (line ~103)
  - `getDocumentOCR()` method (line ~270)

- `backend/src/main/java/com/bpdb/dms/entity/Document.java`
  - `extractedText` field (line ~67)

- `backend/src/main/java/com/bpdb/dms/service/FileUploadService.java`
  - `processDocumentAsync()` method (line ~385)
  - OCR processing and saving (line ~444)

### Frontend:
- `frontend/src/components/DocumentViewer.tsx`
  - `loadDocumentPreview()` method (line ~193)
  - OCR text extraction (line ~229)
  - `renderOCRText()` method (line ~437)

- `frontend/src/services/documentService.ts`
  - `getDocumentById()` method (line ~135)
  - `getDocumentOCR()` method (line ~141)

## Testing Checklist

- [ ] Upload an image file → Verify OCR runs
- [ ] Upload a PDF file → Verify OCR runs
- [ ] Upload a Word document → Verify OCR is skipped
- [ ] View document with OCR text → Verify text displays
- [ ] View document without OCR text → Verify processing message
- [ ] Check database → Verify `extractedText` is populated
- [ ] Check API response → Verify `extractedText` in response
- [ ] Test fallback → Verify Elasticsearch fallback works

## Success Criteria

✅ OCR text displays from `document.extractedText` field
✅ No unnecessary API calls when text is available
✅ Graceful fallback when text is not available
✅ Proper error handling and user feedback
✅ Works for both images and PDFs
✅ Skips OCR for other file types

## Next Steps

If implementation is not working:

1. **Check Backend Logs:**
   - Look for OCR initialization messages
   - Check for OCR processing errors
   - Verify document save operations

2. **Check Database:**
   - Verify `extractedText` column exists
   - Check if data is being saved
   - Verify data is not null/empty

3. **Check Frontend:**
   - Verify API calls are successful
   - Check response structure
   - Verify component state updates

4. **Test Endpoints:**
   - Test `/api/documents/{id}` endpoint directly
   - Test `/api/documents/{id}/ocr` endpoint
   - Verify response structure matches expectations

