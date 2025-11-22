#!/bin/bash
# Quick test script to check APP Excel file structure
# This will help verify column names

echo "Checking APP Excel files in uploads directory..."
echo ""

for file in uploads/*.xlsx; do
    if [ -f "$file" ]; then
        echo "File: $file"
        echo "Size: $(stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null) bytes"
        echo ""
    fi
done

echo "To properly test, you would need to:"
echo "1. Upload an APP Excel file through the application"
echo "2. Check the application logs for 'Detected headers in APP document' messages"
echo "3. Check the database for app_document_entries records"
echo ""
echo "Or use a Java test to read the Excel file directly."

