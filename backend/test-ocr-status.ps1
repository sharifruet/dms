# PowerShell script to test OCR status
Write-Host "Testing OCR Configuration..." -ForegroundColor Cyan
Write-Host ""

# Check if Tesseract path exists
$tesseractPath = "C:/Program Files/Tesseract-OCR/tessdata"
$tessdataPath = "C:/Program Files/Tesseract-OCR/tessdata/eng.traineddata"

Write-Host "Checking Tesseract data path: $tesseractPath" -ForegroundColor Yellow
if (Test-Path $tesseractPath) {
    Write-Host "✅ Tesseract data directory exists" -ForegroundColor Green
} else {
    Write-Host "❌ Tesseract data directory NOT found" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Checking English language data: $tessdataPath" -ForegroundColor Yellow
if (Test-Path $tessdataPath) {
    Write-Host "✅ English language data file exists" -ForegroundColor Green
} else {
    Write-Host "❌ English language data file NOT found" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Checking if backend is running..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/health" -Method GET -TimeoutSec 5 -ErrorAction Stop
    if ($response.StatusCode -eq 200) {
        Write-Host "✅ Backend is running" -ForegroundColor Green
        $healthData = $response.Content | ConvertFrom-Json
        Write-Host "   Status: $($healthData.status)" -ForegroundColor Cyan
        Write-Host "   Database: $($healthData.database)" -ForegroundColor Cyan
        
        Write-Host ""
        Write-Host "Checking OCR service status..." -ForegroundColor Yellow
        try {
            $ocrResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/health/ocr" -Method GET -TimeoutSec 5 -ErrorAction Stop
            if ($ocrResponse.StatusCode -eq 200) {
                $ocrData = $ocrResponse.Content | ConvertFrom-Json
                if ($ocrData.available -eq $true) {
                    Write-Host "✅ OCR service is available and ready" -ForegroundColor Green
                    Write-Host "   Status: $($ocrData.status)" -ForegroundColor Cyan
                    Write-Host "   Message: $($ocrData.message)" -ForegroundColor Cyan
                } else {
                    Write-Host "❌ OCR service is not available" -ForegroundColor Red
                    Write-Host "   Status: $($ocrData.status)" -ForegroundColor Red
                    Write-Host "   Message: $($ocrData.message)" -ForegroundColor Red
                    if ($ocrData.error) {
                        Write-Host "   Error: $($ocrData.error)" -ForegroundColor Red
                    }
                }
            }
        } catch {
            Write-Host "⚠️  Could not check OCR status: $($_.Exception.Message)" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "❌ Backend is not running or not accessible" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please start the backend first to test OCR functionality." -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "OCR Configuration Check Complete!" -ForegroundColor Green
Write-Host ""
Write-Host "To test OCR with a document, you can:" -ForegroundColor Cyan
Write-Host "1. Upload a document via the API or frontend" -ForegroundColor White
Write-Host "2. Use the reprocess-ocr endpoint for existing documents" -ForegroundColor White
Write-Host "3. Check OCR results via GET /api/documents/{id}/ocr" -ForegroundColor White

