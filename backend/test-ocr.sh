#!/bin/bash
# Quick script to test OCR extraction for document 13

echo "🔐 Logging in..."
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' | \
  python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('token', ''))" 2>/dev/null)

if [ -z "$TOKEN" ]; then
  echo "❌ Login failed. Is the backend running on port 8080?"
  exit 1
fi

echo "✅ Login successful"
echo ""

DOC_ID=${1:-13}
echo "🔄 Triggering OCR reprocessing for document $DOC_ID..."
curl -s -X POST http://localhost:8080/api/documents/$DOC_ID/reprocess-ocr \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | python3 -m json.tool
echo ""

echo "⏳ Waiting 15 seconds for OCR processing..."
sleep 15

echo "📄 Checking OCR results..."
OCR_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/documents/$DOC_ID/ocr)

TEXT=$(echo "$OCR_RESPONSE" | python3 -c \
  "import sys,json; d=json.load(sys.stdin); print(d.get('ocrText', '') or '')" 2>/dev/null)
CONF=$(echo "$OCR_RESPONSE" | python3 -c \
  "import sys,json; d=json.load(sys.stdin); print(d.get('ocrConfidence', 0))" 2>/dev/null)
ERR=$(echo "$OCR_RESPONSE" | python3 -c \
  "import sys,json; d=json.load(sys.stdin); print(d.get('ocrError', 'None'))" 2>/dev/null)

TEXT_LEN=${#TEXT}

echo "Text length: $TEXT_LEN"
echo "Confidence: $CONF"
echo "Error: $ERR"
echo ""

if [ "$TEXT_LEN" -gt 10 ]; then
  echo "✅ OCR text extracted successfully!"
  echo ""
  echo "First 500 characters:"
  echo "$TEXT" | head -c 500
  echo "..."
elif [ "$ERR" != "None" ] && [ "$ERR" != "null" ] && [ -n "$ERR" ]; then
  echo "❌ OCR failed: $ERR"
else
  echo "⏳ OCR may still be processing or returned empty text."
  echo "   Text length: $TEXT_LEN"
fi

