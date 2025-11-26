# PowerShell script to check document OCR status
param(
    [Parameter(Mandatory=$true)]
    [int]$DocumentId
)

Write-Host "Checking OCR status for document ID: $DocumentId" -ForegroundColor Cyan
Write-Host ""

# Check document details
Write-Host "Document Details:" -ForegroundColor Yellow
$docResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/documents/$DocumentId" -Method GET -ErrorAction SilentlyContinue

if ($docResponse.StatusCode -eq 200) {
    $docData = $docResponse.Content | ConvertFrom-Json
    $doc = $docData.document
    
    Write-Host "  File Name: $($doc.fileName)" -ForegroundColor White
    Write-Host "  MIME Type: $($doc.mimeType)" -ForegroundColor White
    Write-Host "  Document Type: $($doc.documentType)" -ForegroundColor White
    
    $extractedText = $doc.extractedText
    if ($extractedText) {
        $textLength = $extractedText.Length
        Write-Host "  Extracted Text Length: $textLength characters" -ForegroundColor Green
        Write-Host "  First 100 chars: $($extractedText.Substring(0, [Math]::Min(100, $textLength)))" -ForegroundColor Gray
    } else {
        Write-Host "  Extracted Text: NULL or EMPTY" -ForegroundColor Red
    }
    
    Write-Host ""
    Write-Host "OCR Response:" -ForegroundColor Yellow
    Write-Host "  OCR Text from API: $($docData.ocrText)" -ForegroundColor White
    Write-Host "  OCR Confidence: $($docData.ocrConfidence)" -ForegroundColor White
    Write-Host "  OCR Processing: $($docData.ocrProcessing)" -ForegroundColor White
    
    if ($docData.ocrError) {
        Write-Host "  OCR Error: $($docData.ocrError)" -ForegroundColor Red
    }
} else {
    Write-Host "Failed to fetch document. Status: $($docResponse.StatusCode)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Checking OCR endpoint directly..." -ForegroundColor Yellow
$ocrResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/documents/$DocumentId/ocr" -Method GET -ErrorAction SilentlyContinue

if ($ocrResponse.StatusCode -eq 200) {
    $ocrData = $ocrResponse.Content | ConvertFrom-Json
    Write-Host "  OCR Text: $($ocrData.ocrText)" -ForegroundColor White
    Write-Host "  OCR Confidence: $($ocrData.ocrConfidence)" -ForegroundColor White
    Write-Host "  OCR Processing: $($ocrData.ocrProcessing)" -ForegroundColor White
    if ($ocrData.ocrError) {
        Write-Host "  OCR Error: $($ocrData.ocrError)" -ForegroundColor Red
    }
} else {
    Write-Host "Failed to fetch OCR data. Status: $($ocrResponse.StatusCode)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Recommendations:" -ForegroundColor Cyan
Write-Host "1. Check backend logs for OCR processing messages" -ForegroundColor White
Write-Host "2. Verify file type is image/* or application/pdf" -ForegroundColor White
Write-Host "3. Check OCR service status: GET /api/health/ocr" -ForegroundColor White
Write-Host "4. Try reprocessing OCR: POST /api/documents/$DocumentId/reprocess-ocr" -ForegroundColor White

