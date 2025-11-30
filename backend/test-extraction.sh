#!/bin/bash

# Script to test procurement description extraction
# Usage: ./test-extraction.sh [document_id]

DOCUMENT_ID=${1:-1}

echo "=== Testing Procurement Description Extraction ==="
echo "Document ID: $DOCUMENT_ID"
echo ""

# First, get document info
echo "1. Getting document information..."
DOC_INFO=$(curl -s -X GET "http://localhost:8080/api/documents/$DOCUMENT_ID" \
  -H "Content-Type: application/json" 2>&1)

if echo "$DOC_INFO" | grep -q "error\|not found"; then
    echo "❌ Error: Document not found or error occurred"
    echo "$DOC_INFO"
    exit 1
fi

DOC_TYPE=$(echo "$DOC_INFO" | grep -o '"documentType":"[^"]*"' | cut -d'"' -f4)
echo "   Document Type: $DOC_TYPE"

HAS_TEXT=$(echo "$DOC_INFO" | grep -o '"extractedText":"[^"]*"' | wc -l)
if [ "$HAS_TEXT" -eq 0 ]; then
    echo "⚠️  Warning: Document may not have extracted text"
else
    echo "   ✓ Document has extracted text"
fi

# Get current metadata
echo ""
echo "2. Getting current metadata..."
METADATA_BEFORE=$(curl -s -X GET "http://localhost:8080/api/documents/$DOCUMENT_ID" \
  -H "Content-Type: application/json" | grep -o '"metadata":{[^}]*}' || echo "{}")

PROC_DESC_BEFORE=$(echo "$METADATA_BEFORE" | grep -o '"procurementDescription":"[^"]*"' | cut -d'"' -f4 || echo "")
if [ -n "$PROC_DESC_BEFORE" ]; then
    echo "   Current procurementDescription: ${PROC_DESC_BEFORE:0:100}..."
else
    echo "   No procurementDescription found in metadata"
fi

# Trigger extraction
echo ""
echo "3. Triggering metadata extraction..."
EXTRACTION_RESULT=$(curl -s -X POST "http://localhost:8080/api/documents/$DOCUMENT_ID/extract-metadata" \
  -H "Content-Type: application/json" 2>&1)

if echo "$EXTRACTION_RESULT" | grep -q "success.*true"; then
    echo "   ✓ Extraction completed successfully"
else
    echo "   ❌ Extraction failed or returned error"
    echo "$EXTRACTION_RESULT"
    exit 1
fi

# Get updated metadata
echo ""
echo "4. Getting updated metadata..."
METADATA_AFTER=$(curl -s -X GET "http://localhost:8080/api/documents/$DOCUMENT_ID" \
  -H "Content-Type: application/json" | grep -o '"metadata":{[^}]*}' || echo "{}")

PROC_DESC_AFTER=$(echo "$EXTRACTION_RESULT" | grep -o '"procurementDescription":"[^"]*"' | cut -d'"' -f4 || echo "")

if [ -z "$PROC_DESC_AFTER" ]; then
    # Try to get it from the full document response
    FULL_DOC=$(curl -s -X GET "http://localhost:8080/api/documents/$DOCUMENT_ID" \
      -H "Content-Type: application/json")
    PROC_DESC_AFTER=$(echo "$FULL_DOC" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if 'metadata' in data and 'procurementDescription' in data['metadata']:
        print(data['metadata']['procurementDescription'])
except:
    pass
" 2>/dev/null || echo "")
fi

if [ -n "$PROC_DESC_AFTER" ] && [ "$PROC_DESC_AFTER" != "null" ]; then
    echo "   ✓ SUCCESS: procurementDescription extracted!"
    echo ""
    echo "   Extracted value (first 300 chars):"
    echo "   ${PROC_DESC_AFTER:0:300}..."
    echo ""
    echo "   Length: ${#PROC_DESC_AFTER} characters"
    
    # Verify labels are removed
    if echo "$PROC_DESC_AFTER" | grep -qi "tender/proposal"; then
        echo "   ⚠️  Warning: Still contains 'Tender/Proposal'"
    fi
    if echo "$PROC_DESC_AFTER" | grep -qi "package no. and"; then
        echo "   ⚠️  Warning: Still contains 'Package No. and'"
    fi
    if echo "$PROC_DESC_AFTER" | grep -qi "^description :"; then
        echo "   ⚠️  Warning: Starts with 'Description :'"
    fi
    
    echo ""
    echo "✅ Test PASSED: Procurement description extracted successfully!"
else
    echo "   ❌ FAILED: procurementDescription was not extracted"
    echo ""
    echo "   Full extraction response:"
    echo "$EXTRACTION_RESULT" | head -50
    echo ""
    echo "   This could mean:"
    echo "   1. The pattern didn't match the OCR text format"
    echo "   2. The field is not configured in document_type_fields"
    echo "   3. The document type is not TENDER_NOTICE"
    echo ""
    echo "   Check application logs for more details."
    exit 1
fi

echo ""
echo "=== Test Complete ==="

