# OCR Implementation for Document Upload

## Summary
Updated the document upload process to run OCR **only for images and PDFs** and store the extracted text in the `extractedText` field of the document table.

## Changes Made

### File: `backend/src/main/java/com/bpdb/dms/service/FileUploadService.java`

#### Key Changes:
1. **File Type Check**: Added explicit check to determine if file is an image or PDF before running OCR
2. **OCR Execution**: OCR now runs **only** for:
   - Image files (content type starts with `image/`)
   - PDF files (content type is `application/pdf`)
3. **Extracted Text Storage**: OCR extracted text is stored in `document.extractedText` field and saved to database
4. **Error Handling**: Proper error handling with empty extracted text saved on failure

#### Implementation Details:

```java
// Check if file type requires OCR (images and PDFs only)
String contentType = file.getContentType();
boolean isImage = contentType != null && contentType.startsWith("image/");
boolean isPDF = "application/pdf".equals(contentType);
boolean shouldRunOCR = isImage || isPDF;

if (!shouldRunOCR) {
    // Skip OCR for non-image/PDF files
    logger.info("Skipping OCR processing for document {} - file type: {} (OCR only runs for images and PDFs)", 
               documentId, contentType);
    return;
}

// Run OCR and store extracted text
if (ocrResult != null && ocrResult.isSuccess()) {
    String extractedText = ocrResult.getExtractedText() != null ? ocrResult.getExtractedText() : "";
    managedDocument.setExtractedText(extractedText);
    documentRepository.save(managedDocument);
}
```

## Behavior

### For Images and PDFs:
1. ✅ OCR is executed using Tesseract
2. ✅ Extracted text is stored in `document.extractedText` field
3. ✅ Document is saved to database with extracted text
4. ✅ Text is indexed in Elasticsearch for search

### For Other File Types:
1. ✅ OCR is **skipped** (not executed)
2. ✅ Document is still saved and indexed
3. ✅ `extractedText` field remains empty/null

### Error Handling:
- If OCR service is unavailable: Empty `extractedText` is saved
- If OCR processing fails: Empty `extractedText` is saved with error metadata
- If OCR throws exception: Empty `extractedText` is saved with error details

## Database Schema

The `documents` table already has the `extracted_text` column:
```sql
extracted_text TEXT
```

This field stores the OCR-extracted text for images and PDFs.

## Testing

To test the implementation:

1. **Upload an Image**:
   - Upload a JPG/PNG image file
   - Check that `extractedText` field is populated in the database
   - Verify OCR text is searchable

2. **Upload a PDF**:
   - Upload a PDF file
   - Check that `extractedText` field is populated in the database
   - Verify OCR text is searchable

3. **Upload Other File Types**:
   - Upload a Word document, Excel file, or text file
   - Verify that OCR is **not** executed (check logs)
   - Verify that `extractedText` field remains empty

## Logging

The implementation includes detailed logging:
- `"Running OCR for document {} - file type: {}"` - When OCR starts
- `"OCR completed for document {} - extracted text length: {}"` - When OCR succeeds
- `"Skipping OCR processing for document {} - file type: {} (OCR only runs for images and PDFs)"` - When OCR is skipped
- Error logs for OCR failures

## Related Files

- `backend/src/main/java/com/bpdb/dms/service/OCRService.java` - OCR service implementation
- `backend/src/main/java/com/bpdb/dms/entity/Document.java` - Document entity with `extractedText` field
- `backend/src/main/resources/application.properties` - OCR configuration

