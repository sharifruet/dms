-- SQL script to test procurement description extraction
-- Run with: psql -h localhost -U dms_user -d dms_db -f test-procurement-extraction.sql

\echo '=== Testing Procurement Description Extraction ==='
\echo ''

-- Get document information
\echo '1. Document Information:'
SELECT 
    id,
    document_type,
    file_name,
    LENGTH(extracted_text) as extracted_text_length,
    CASE 
        WHEN extracted_text IS NULL OR extracted_text = '' THEN 'NO'
        ELSE 'YES'
    END as has_extracted_text
FROM documents
ORDER BY id
LIMIT 1;

\echo ''
\echo '2. Sample of extracted text around "Package":'
SELECT 
    id,
    SUBSTRING(
        extracted_text,
        GREATEST(1, POSITION(LOWER('package') IN LOWER(extracted_text)) - 150),
        500
    ) as text_around_package
FROM documents
WHERE extracted_text IS NOT NULL 
  AND LOWER(extracted_text) LIKE '%package%'
ORDER BY id
LIMIT 1;

\echo ''
\echo '3. Current metadata for procurementDescription:'
SELECT 
    d.id as document_id,
    d.document_type,
    dm.key,
    CASE 
        WHEN LENGTH(dm.value) > 200 THEN LEFT(dm.value, 200) || '...'
        ELSE dm.value
    END as value_preview,
    LENGTH(dm.value) as value_length
FROM documents d
LEFT JOIN document_metadata dm ON dm.document_id = d.id AND dm.key = 'procurementDescription'
ORDER BY d.id
LIMIT 1;

\echo ''
\echo '4. Checking if procurementDescription field exists in document_type_fields:'
SELECT 
    document_type,
    field_key,
    field_label,
    is_ocr_mappable,
    ocr_pattern IS NOT NULL as has_pattern
FROM document_type_fields
WHERE document_type = 'TENDER_NOTICE'
  AND field_key = 'procurementDescription';

\echo ''
\echo '=== Test Complete ==='
\echo ''
\echo 'If procurementDescription is NULL or empty, extraction may not have run yet.'
\echo 'Check application logs for extraction details.'

