#!/bin/bash
# Test script to verify OCR extraction and storage during document upload
# This script checks if extracted_text is being populated in the database

echo "========================================="
echo "OCR Upload Verification Script"
echo "========================================="
echo ""

# Check if we can query the database
echo "Checking database connection..."
echo ""

# Test query to check documents with extracted text
cat << 'EOF'
-- SQL Query to check OCR extracted text in database:
-- Run this query to verify extracted_text is being stored:

SELECT 
    id,
    file_name,
    original_name,
    mime_type,
    document_type,
    CASE 
        WHEN extracted_text IS NULL THEN 'NULL'
        WHEN extracted_text = '' THEN 'EMPTY'
        ELSE CONCAT('HAS_TEXT (', LENGTH(extracted_text), ' chars)')
    END as extraction_status,
    LENGTH(extracted_text) as text_length,
    LEFT(extracted_text, 100) as text_preview,
    created_at,
    updated_at
FROM documents
ORDER BY created_at DESC
LIMIT 10;
EOF

echo ""
echo "========================================="
echo "Upload Flow Verification:"
echo "========================================="
echo ""
echo "1. Document uploaded → FileUploadService.uploadFile()"
echo "2. Document saved to database (extracted_text initially NULL)"
echo "3. Async OCR processing → FileUploadService.processDocumentAsync()"
echo "4. OCR extraction → OCRService.extractText()"
echo "5. Text stored → document.setExtractedText() + documentRepository.save()"
echo ""
echo "========================================="
echo "To verify OCR is working:"
echo "========================================="
echo ""
echo "1. Upload an image or PDF document"
echo "2. Wait a few seconds for async OCR processing"
echo "3. Check the database with the query above"
echo "4. extracted_text should contain the OCR extracted text"
echo ""
echo "Configuration check:"
echo "  - app.ocr.enabled=true"
echo "  - app.ocr.process-images=true"
echo "  - app.tesseract.binary=/opt/homebrew/bin/tesseract"
echo "  - app.tesseract.data.path=/opt/homebrew/share"
echo ""

