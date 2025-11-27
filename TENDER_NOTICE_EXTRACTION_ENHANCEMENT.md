# Tender Notice Metadata Extraction Enhancement

## Overview

Enhanced the metadata extraction system to capture more structured fields from Tender Notice OCR text, based on the sample data format.

## New Fields Extracted

The system now extracts the following fields from Tender Notice documents:

### 1. **Tender/Proposal ID**
- Pattern: `Tender/Proposal ID : 1156325`
- Extracted as: `tenderId` or `proposalId`

### 2. **Invitation Reference No**
- Pattern: `Invitation Reference No. : 27.11.0000.304.26.103.25`
- Extracted as: `invitationReferenceNo`

### 3. **App ID**
- Pattern: `App ID: 217617`
- Extracted as: `appId`

### 4. **Ministry**
- Pattern: `Ministry : Ministry of Energy, Power Division`
- Extracted as: `ministry`

### 5. **Organization**
- Pattern: `Organization : Bangladesh Power Development Board`
- Extracted as: `organization`

### 6. **Procuring Entity**
- Pattern: `Procuring Entity Name : Directorate of Purchase`
- Extracted as: `procuringEntity`

### 7. **Document Price**
- Pattern: `Tender/Proposal Document Price (In BDT) : 4000`
- Extracted as: `documentPrice`

### 8. **Closing Date and Time**
- Pattern: `Tender/Proposal Closing Date and Time : 22-Oct-2025 14:00`
- Extracted as: `closingDate`
- Normalized format: `2025-10-22 14:00`

### 9. **Opening Date and Time**
- Pattern: `Tender/Proposal Opening Date and Time : 22-Oct-2025 14:00`
- Extracted as: `openingDate`
- Normalized format: `2025-10-22 14:00`

### 10. **Publication Date and Time**
- Pattern: `Scheduled Tender/Proposal Document last selling / downloading Date and Time : 24-Sep-2025 13:00`
- Extracted as: `publicationDate`
- Normalized format: `2025-09-24 13:00`

## Implementation Details

### New Extraction Patterns

Added the following regex patterns in `DocumentMetadataService.java`:

1. `TENDER_ID_PATTERN` - Matches "Tender/Proposal ID : 1156325"
2. `INVITATION_REF_PATTERN` - Matches "Invitation Reference No. : 27.11.0000.304.26.103.25"
3. `APP_ID_PATTERN` - Matches "App ID: 217617"
4. `MINISTRY_PATTERN` - Matches "Ministry : Ministry of Energy, Power Division"
5. `ORGANIZATION_PATTERN` - Matches "Organization : Bangladesh Power Development Board"
6. `PROCURING_ENTITY_PATTERN` - Matches "Procuring Entity Name : Directorate of Purchase"
7. `DOCUMENT_PRICE_PATTERN` - Matches "Tender/Proposal Document Price (In BDT) : 4000"
8. `CLOSING_DATE_TIME_PATTERN` - Matches "Tender/Proposal Closing Date and Time : 22-Oct-2025 14:00"
9. `OPENING_DATE_TIME_PATTERN` - Matches "Tender/Proposal Opening Date and Time : 22-Oct-2025 14:00"
10. `PUBLICATION_DATE_TIME_PATTERN` - Matches publication/downloading dates

### Date-Time Normalization

Added `normalizeDateTime()` method to convert OCR date-time strings to standardized format:
- Input: `"22-Oct-2025 14:00"`
- Output: `"2025-10-22 14:00"`

## Sample Data Mapping

Based on the provided sample Tender Notice OCR text:

| Field | Sample Value | Extracted As |
|-------|--------------|--------------|
| Tender/Proposal ID | 1156325 | `tenderId` |
| Invitation Reference No | 27.11.0000.304.26.103.25 | `invitationReferenceNo` |
| App ID | 217617 | `appId` |
| Ministry | Ministry of Energy, Power Division | `ministry` |
| Organization | Bangladesh Power Development Board | `organization` |
| Procuring Entity | Directorate of Purchase | `procuringEntity` |
| Document Price | 4000 | `documentPrice` |
| Closing Date | 22-Oct-2025 14:00 | `closingDate` |
| Opening Date | 22-Oct-2025 14:00 | `openingDate` |
| Publication Date | 24-Sep-2025 13:00 | `publicationDate` |

## How It Works

1. **OCR Extraction**: Text is extracted from PDF/image using Tesseract
2. **Metadata Extraction**: `DocumentMetadataService.extractMetadataFromText()` is called
3. **Pattern Matching**: Regex patterns match structured fields in the OCR text
4. **Storage**: Extracted fields are stored in `document_metadata` table
5. **Access**: Fields are available via document metadata API

## Testing

To verify the extraction is working:

1. **Upload a Tender Notice PDF**
2. **Wait for OCR processing** (check logs)
3. **Check document metadata**:
   ```sql
   SELECT key, value 
   FROM document_metadata 
   WHERE document_id = {your_document_id}
   ORDER BY key;
   ```

4. **Expected fields**:
   - `tenderId`: "1156325"
   - `invitationReferenceNo`: "27.11.0000.304.26.103.25"
   - `appId`: "217617"
   - `ministry`: "Ministry of Energy, Power Division"
   - `organization`: "Bangladesh Power Development Board"
   - `procuringEntity`: "Directorate of Purchase"
   - `documentPrice`: "4000"
   - `closingDate`: "2025-10-22 14:00"
   - `openingDate`: "2025-10-22 14:00"
   - `publicationDate`: "2025-09-24 13:00"

## Code Location

**File**: `backend/src/main/java/com/bpdb/dms/service/DocumentMetadataService.java`

- Patterns defined: Lines ~75-105
- Extraction logic: Lines ~149-200
- Date-time normalization: Lines ~475-510

## Status

✅ **Enhanced** - Added 10 new extraction patterns for Tender Notice fields
✅ **Tested** - Patterns match the sample data format
✅ **Ready** - Code changes complete and compiled

The system will now automatically extract these structured fields when processing Tender Notice documents via OCR.

