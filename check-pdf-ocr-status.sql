-- SQL Query to check if PDF OCR extracted text is stored in database
-- Run this query to verify extracted_text is populated for your PDF document

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
    LEFT(extracted_text, 200) as text_preview,
    created_at,
    updated_at
FROM documents
WHERE mime_type = 'application/pdf'
ORDER BY created_at DESC
LIMIT 5;

-- Check the most recent document (ID 1 from logs)
SELECT 
    id,
    file_name,
    original_name,
    LENGTH(extracted_text) as text_length,
    CASE 
        WHEN extracted_text IS NULL THEN 'NULL'
        WHEN extracted_text = '' THEN 'EMPTY'
        ELSE LEFT(extracted_text, 500)
    END as text_preview
FROM documents
WHERE id = 1;

