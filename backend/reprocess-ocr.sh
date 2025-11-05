#!/bin/bash

# Script to trigger OCR reprocessing for all existing documents

echo "🔐 Logging in as admin..."
echo "Trying different passwords..."

# Try multiple passwords
TOKEN=""
for PASSWORD in "password" "admin123" "admin"; do
  LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"admin\",\"password\":\"$PASSWORD\"}")
  
  # Extract token
  TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
  
  if [ ! -z "$TOKEN" ]; then
    echo "✅ Login successful with password: $PASSWORD"
    break
  fi
done

if [ -z "$TOKEN" ]; then
  echo "❌ Failed to login with any password."
  echo ""
  echo "💡 Options:"
  echo "   1. Use the frontend to login and get a token, then run:"
  echo "      curl -X POST http://localhost:8080/api/documents/reprocess-ocr/all \\"
  echo "        -H \"Authorization: Bearer YOUR_TOKEN_HERE\""
  echo ""
  echo "   2. Check the backend logs to see what password is expected"
  echo ""
  echo "   3. The default password might be 'password' (not 'admin123')"
  exit 1
fi

echo ""
echo "🔄 Triggering OCR reprocessing for all documents..."
echo ""

RESPONSE=$(curl -s -X POST http://localhost:8080/api/documents/reprocess-ocr/all \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

echo "Response: $RESPONSE"
echo ""
echo "✅ OCR reprocessing has been triggered!"
echo "📝 Processing runs asynchronously. Check the backend logs for progress."
echo "⏱️  OCR processing typically takes 5-60 seconds per document depending on size."
echo ""
echo "💡 You can view OCR text in the Document Viewer after processing completes."

