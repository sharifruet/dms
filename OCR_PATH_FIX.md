# OCR Path Fix - Tesseract Language Data File Issue

## Problem Identified

From the logs, the error is:
```
Error opening data file C:\Program Files\Tesseract-OCR/eng.traineddata
Please make sure the TESSDATA_PREFIX environment variable is set to your "tessdata" directory.
Failed loading language 'eng'
Tesseract couldn't load any languages!
```

**Root Cause:**
- Tesseract is looking for `C:\Program Files\Tesseract-OCR/eng.traineddata` (wrong location)
- File actually exists at: `C:\Program Files\Tesseract-OCR\tessdata\eng.traineddata`
- The native Tesseract library needs `TESSDATA_PREFIX` set correctly

## Fixes Applied

### 1. Path Normalization
- Added path normalization for Windows (forward slashes → backslashes)
- Ensures consistent path format

### 2. TESSDATA_PREFIX System Property
- Set `TESSDATA_PREFIX` as a system property before OCR operations
- Points to the parent directory of `tessdata` folder

### 3. Pre-OCR Path Verification
- Added `ensureTessdataPrefix()` method
- Called before each OCR operation to ensure path is set

### 4. Better Error Handling
- Improved logging to show exact paths being used
- Verifies language data file exists before marking OCR as available

## Next Steps

1. **Restart the backend** to apply the fixes
2. **Upload a new document** (image or PDF)
3. **Check the logs** for:
   - `TESSDATA_PREFIX set to: C:\Program Files\Tesseract-OCR`
   - `OCR completed for document {id} - extracted text length: {n}`
   - `Successfully saved extracted text to database`

## Verification

After restart, check:
1. OCR initialization log should show: `traineddata exists: true`
2. When uploading, logs should show successful OCR processing
3. Database `extracted_text` column should be populated

## If Still Not Working

The issue might be that tess4j's native library doesn't respect the system property. In that case, we may need to:
1. Set TESSDATA_PREFIX as an environment variable before starting the JVM
2. Or use the external tesseract CLI fallback (which already sets TESSDATA_PREFIX correctly)

