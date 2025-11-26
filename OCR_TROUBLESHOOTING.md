# OCR Troubleshooting Guide

## Issue: extractedText is Blank in Database

### Root Cause
The most common issue is that the `MultipartFile` input stream is consumed or closed before the async OCR processing runs. This happens because:
1. The file is saved to disk first
2. The async method is called with the original MultipartFile
3. By the time async processing runs, the stream may be closed

### Solution Implemented
The code now:
1. ✅ Reads the file from the saved file path instead of using MultipartFile directly
2. ✅ Creates a new MultipartFile wrapper from the saved file path
3. ✅ Uses the mime type from the document entity
4. ✅ Adds better logging to diagnose issues

### How to Verify

#### 1. Check Backend Logs
Look for these log messages when uploading a document:

**Success:**
```
Starting async processing for document: {id}
Processing document {id} - Content type: application/pdf, File path: ...
Running OCR for document {id} - file type: application/pdf
OCR completed for document {id} - extracted text length: {length}, confidence: {confidence}
Successfully saved extracted text to database for document {id}
```

**Failure Indicators:**
```
Skipping OCR processing for document {id} - file type: {type} (OCR only runs for images and PDFs)
Skipping OCR processing for document {id} because OCR service is unavailable
OCR extraction threw an error for document {id}: {error}
OCR completed for document {id} but extracted text is empty
```

#### 2. Check Database
```sql
SELECT id, file_name, mime_type, extracted_text, 
       LENGTH(extracted_text) as text_length,
       created_at
FROM documents 
WHERE id = {your_document_id};
```

**Expected:**
- `mime_type` should be `image/*` or `application/pdf`
- `extracted_text` should contain text (not NULL or empty)
- `text_length` should be > 0

#### 3. Check File Type
The document must be:
- ✅ An image file (content type starts with `image/`)
- ✅ A PDF file (content type is `application/pdf`)

Other file types will skip OCR processing.

#### 4. Check OCR Service Status
```bash
curl http://localhost:8080/api/health/ocr
```

**Expected Response:**
```json
{
  "available": true,
  "status": "READY",
  "message": "OCR service is ready"
}
```

### Common Issues and Solutions

#### Issue 1: File Type Not Detected
**Symptom:** Log shows "Skipping OCR processing - file type: ..."

**Solution:**
- Verify the file is actually an image or PDF
- Check `mime_type` in database
- Ensure file extension matches content type

#### Issue 2: OCR Service Unavailable
**Symptom:** Log shows "OCR service is unavailable"

**Solution:**
- Check OCR path configuration: `app.tesseract.data.path`
- Verify Tesseract is installed
- Check OCR status endpoint: `/api/health/ocr`

#### Issue 3: OCR Processing Fails
**Symptom:** Log shows "OCR extraction threw an error"

**Solution:**
- Check Tesseract installation
- Verify language data files exist
- Check file permissions
- Review full error message in logs

#### Issue 4: Extracted Text is Empty
**Symptom:** OCR completes but `extractedText` is empty

**Possible Causes:**
- Image/PDF has no text (scanned image with no text)
- OCR quality is poor
- File is corrupted

**Solution:**
- Try with a different document that has clear text
- Check OCR confidence score
- Verify file is not corrupted

#### Issue 5: Text Not Saved to Database
**Symptom:** OCR completes but database shows blank

**Solution:**
- Check logs for "Successfully saved extracted text" message
- Verify database transaction committed
- Check for database errors in logs
- Verify `extracted_text` column exists

### Testing Steps

1. **Upload a Test Document:**
   - Use a simple PDF or image with clear text
   - Wait for upload to complete

2. **Check Logs:**
   - Look for OCR processing messages
   - Verify no errors occurred

3. **Check Database:**
   - Query the document
   - Verify `extracted_text` is populated

4. **Check Frontend:**
   - Open document viewer
   - Click "OCR Text" tab
   - Verify text displays

### Debugging Commands

**Check document in database:**
```sql
SELECT id, file_name, mime_type, 
       CASE 
         WHEN extracted_text IS NULL THEN 'NULL'
         WHEN extracted_text = '' THEN 'EMPTY'
         ELSE CONCAT('HAS_TEXT (', LENGTH(extracted_text), ' chars)')
       END as text_status,
       created_at
FROM documents 
ORDER BY created_at DESC 
LIMIT 10;
```

**Check OCR configuration:**
```bash
# Windows PowerShell
Test-Path "C:/Program Files/Tesseract-OCR/tessdata/eng.traineddata"
```

**Check backend logs:**
```bash
# Look for OCR-related messages
grep -i "ocr\|tesseract" backend/logs/application.log
```

### Next Steps if Still Not Working

1. **Enable Debug Logging:**
   Add to `application.properties`:
   ```properties
   logging.level.com.bpdb.dms.service.FileUploadService=DEBUG
   logging.level.com.bpdb.dms.service.OCRService=DEBUG
   ```

2. **Test OCR Manually:**
   - Use the reprocess OCR endpoint
   - Check if it works: `POST /api/documents/{id}/reprocess-ocr`

3. **Check File Path:**
   - Verify file exists at the path stored in database
   - Check file permissions
   - Verify file is readable

4. **Review Full Stack Trace:**
   - Look for exceptions in logs
   - Check for transaction rollbacks
   - Verify no database constraint violations

