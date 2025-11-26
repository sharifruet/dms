# Diagnosing OCR extractedText Blank Issue

## Quick Diagnostic Steps

### 1. Check Backend Logs
Look for these log messages when you upload a document:

**What to look for:**
```
Starting async processing for document: {id}
Processing document {id} - Content type: {type}, File path: ...
```

**Possible outcomes:**

**A) File type not image/PDF:**
```
Skipping OCR processing for document {id} - file type: {type} (OCR only runs for images and PDFs)
```
**Solution:** Upload an image (JPG, PNG) or PDF file

**B) OCR service unavailable:**
```
Skipping OCR processing for document {id} because OCR service is unavailable
```
**Solution:** Check OCR configuration and Tesseract installation

**C) OCR running:**
```
Running OCR for document {id} - file type: {type}
OCR result for document {id} - Success: true/false, Text length: {n}, ...
Successfully saved extracted text to database for document {id}
```

**D) OCR error:**
```
OCR extraction threw an error for document {id}: {error message}
```
**Solution:** Check the error message and fix the issue

### 2. Check Document in Database

Run this SQL query:
```sql
SELECT 
    id, 
    file_name, 
    mime_type, 
    document_type,
    CASE 
        WHEN extracted_text IS NULL THEN 'NULL'
        WHEN extracted_text = '' THEN 'EMPTY'
        ELSE CONCAT('HAS_TEXT (', LENGTH(extracted_text), ' chars)')
    END as text_status,
    created_at
FROM documents 
WHERE id = {your_document_id};
```

**What to check:**
- `mime_type` should be `image/*` or `application/pdf`
- `text_status` should show `HAS_TEXT` if OCR worked

### 3. Check OCR Service Status

```bash
# PowerShell
Invoke-WebRequest -Uri "http://localhost:8080/api/health/ocr" | Select-Object -ExpandProperty Content
```

**Expected:**
```json
{
  "available": true,
  "status": "READY",
  "message": "OCR service is ready"
}
```

### 4. Test with Diagnostic Script

Use the provided script:
```powershell
.\backend\check-document-ocr.ps1 -DocumentId {your_document_id}
```

## Common Issues and Solutions

### Issue 1: File Type Not Detected
**Symptom:** Log shows "Skipping OCR processing - file type: ..."

**Check:**
- What file did you upload? (Word doc, Excel, etc.)
- OCR only runs for images and PDFs

**Solution:**
- Upload a JPG, PNG, or PDF file
- Check `mime_type` in database matches

### Issue 2: OCR Service Not Available
**Symptom:** Log shows "OCR service is unavailable"

**Check:**
- Tesseract installation
- OCR path configuration
- OCR status endpoint

**Solution:**
- Verify Tesseract is installed: `tesseract --version`
- Check path in `application.properties`: `app.tesseract.data.path`
- Run: `powershell -File backend/test-ocr-status.ps1`

### Issue 3: OCR Runs But Text is Empty
**Symptom:** Log shows "OCR completed" but text is empty

**Possible causes:**
- Document has no text (scanned image with no text)
- Poor image quality
- OCR confidence is low

**Solution:**
- Try with a document that has clear, readable text
- Check OCR confidence in logs
- Verify image quality

### Issue 4: OCR Error
**Symptom:** Log shows "OCR extraction threw an error"

**Check the error message:**
- Tesseract not found → Install Tesseract
- Language data missing → Check `eng.traineddata` exists
- File read error → Check file permissions

## Next Steps

1. **Check the logs** - Look for the messages above
2. **Verify file type** - Must be image or PDF
3. **Check OCR status** - Use `/api/health/ocr` endpoint
4. **Try reprocessing** - Use reprocess OCR endpoint for existing documents

## Reprocess OCR for Existing Document

If you want to retry OCR on an existing document:

```bash
# Login first to get token
# Then:
curl -X POST http://localhost:8080/api/documents/{id}/reprocess-ocr \
  -H "Authorization: Bearer {token}"
```

This will re-run OCR processing for the document.

