# Database Update: Tender Notice Fields with PostgreSQL Regex Patterns

## Overview

Created a Liquibase database migration to update the `document_type_fields` table with PostgreSQL-compatible regex patterns for Tender Notice document fields.

## Migration File

**File**: `backend/src/main/resources/db/changelog/030-update-tender-notice-fields-regex.xml`

This migration:
1. Updates existing Tender Notice fields with correct regex patterns
2. Adds new Tender Notice fields with PostgreSQL regex patterns
3. All patterns are compatible with PostgreSQL `regexp_matches()` function

## Changes Made

### 1. Updated Existing Fields

#### tenderId
- **Old Pattern**: `(?i)tender\s*(id|number|no\.?|#)\s*[:\-]?\s*([A-Z0-9\-/]+)`
- **New Pattern**: `Tender/Proposal ID\s*:\s*([0-9]+)`
- **Matches**: `Tender/Proposal ID : 1156325`

#### tenderDate
- **New Pattern**: `(?:Scheduled\s+)?(?:Tender|Proposal)\s+(?:Document\s+)?(?:last\s+selling\s*/\s*downloading|Publication|last\s+selling|downloading)\s+(?:Date\s+and\s+Time|Date)\s*:\s*(\d{1,2}[\-/]\w+[\-/]\d{4}\s+\d{1,2}:\d{2})`
- **Matches**: `Scheduled Tender/Proposal Document last selling / downloading Date and Time : 24-Sep-2025 13:00`
- **Description**: Tender publication/scheduled date (when tender was published)

#### closingDate
- **New Pattern**: `(?:Tender|Proposal)\s+(?:Closing|Submission)\s+(?:Date\s+and\s+Time|Date)\s*:\s*(\d{1,2}[\-/]\w+[\-/]\d{4}\s+\d{1,2}:\d{2})`
- **Matches**: `Tender/Proposal Closing Date and Time : 22-Oct-2025 14:00`
- **Description**: Tender closing date (last date for submission)

### 2. New Fields Added

| Field Key | Field Label | Regex Pattern | Display Order |
|-----------|-------------|---------------|---------------|
| `invitationReferenceNo` | Invitation Reference No | `Invitation Reference\s+No\.?\s*:\s*([0-9.]+)` | 6 |
| `appId` | App ID | `App ID\s*:\s*([0-9]+)` | 7 |
| `ministry` | Ministry | `Ministry\s*:\s*([^\n:]+?)(?:\s*Division|$)` | 8 |
| `organization` | Organization | `Organization\s*:\s*([^\n:]+?)(?:\s*Procuring|$)` | 9 |
| `procuringEntity` | Procuring Entity | `Procuring Entity\s+(?:Name\s*)?:\s*([^\n:]+?)(?:\s*Procuring Entity Code|$)` | 10 |
| `documentPrice` | Document Price (BDT) | `(?:Tender|Proposal)\s+(?:Document\s+)?(?:Price|Fees?)\s*\([^)]*\)\s*:\s*([0-9,]+)` | 11 |
| `publicationDate` | Publication Date | `(?:Scheduled\s+)?(?:Tender|Proposal)\s+(?:Document\s+)?(?:last\s+selling\s*/\s*downloading|Publication|last\s+selling|downloading)\s+(?:Date\s+and\s+Time|Date)\s*:\s*(\d{1,2}[\-/]\w+[\-/]\d{4}\s+\d{1,2}:\d{2})` | 12 |
| `openingDate` | Opening Date | `(?:Tender|Proposal)\s+Opening\s+(?:Date\s+and\s+Time|Date)\s*:\s*(\d{1,2}[\-/]\w+[\-/]\d{4}\s+\d{1,2}:\d{2})` | 13 |
| `preTenderMeetingStart` | Pre-Tender Meeting Start | `Pre\s*-\s*(?:Tender|Proposal)\s+(?:meeting|Meeting)\s+Start\s+(?:Date\s+and\s+Time|Date)\s*:\s*(\d{1,2}[\-/]\w+[\-/]\d{4}\s+\d{1,2}:\d{2})` | 14 |
| `preTenderMeetingEnd` | Pre-Tender Meeting End | `Pre\s*-\s*(?:Tender|Proposal)\s+(?:meeting|Meeting)\s+End\s+(?:Date\s+and\s+Time|Date)\s*:\s*(\d{1,2}[\-/]\w+[\-/]\d{4}\s+\d{1,2}:\d{2})` | 15 |
| `tenderSecurityValidUpTo` | Tender Security Valid Up To | `Tender/Proposal\s+Security\s+Valid\s+Up\s+to\s*:\s*(\d{1,2}[\-/]\w+[\-/]\d{4})` | 16 |
| `tenderValidUpTo` | Tender Valid Up To | `Tender/Proposal\s+Valid\s+Up\s+to\s*:\s*(\d{1,2}[\-/]\w+[\-/]\d{4})` | 17 |

## How to Apply

### Option 1: Automatic (Liquibase)
The migration will run automatically when the application starts (if Liquibase is enabled).

### Option 2: Manual SQL
You can run the SQL directly in PostgreSQL:

```sql
-- Update existing fields
UPDATE document_type_fields 
SET ocr_pattern = 'Tender/Proposal ID\s*:\s*([0-9]+)',
    is_ocr_mappable = true,
    description = 'Tender/Proposal ID extracted from OCR text'
WHERE document_type = 'TENDER_NOTICE' 
AND field_key = 'tenderId';

-- Add new fields (see migration file for complete SQL)
INSERT INTO document_type_fields (document_type, field_key, field_label, field_type, is_required, is_ocr_mappable, ocr_pattern, display_order, description)
VALUES 
('TENDER_NOTICE', 'invitationReferenceNo', 'Invitation Reference No', 'text', false, true, 'Invitation Reference\s+No\.?\s*:\s*([0-9.]+)', 6, 'Invitation Reference Number'),
-- ... (see migration file for all fields)
ON CONFLICT (document_type, field_key) DO UPDATE 
SET ocr_pattern = EXCLUDED.ocr_pattern,
    field_label = EXCLUDED.field_label,
    description = EXCLUDED.description;
```

## Verification

After applying the migration, verify the fields are updated:

```sql
-- Check all Tender Notice fields
SELECT 
    field_key,
    field_label,
    ocr_pattern,
    is_ocr_mappable,
    display_order
FROM document_type_fields
WHERE document_type = 'TENDER_NOTICE'
AND is_active = true
ORDER BY display_order;
```

Expected output should show:
- Updated `tenderId`, `tenderDate`, `closingDate` patterns
- New fields: `invitationReferenceNo`, `appId`, `ministry`, `organization`, etc.

## Integration

These regex patterns are used by:

1. **DocumentTypeFieldService** - Extracts metadata using Java Pattern.compile
2. **DatabaseMetadataExtractionService** - Extracts metadata using PostgreSQL regexp_matches()

Both services will use the patterns stored in `document_type_fields.ocr_pattern` column.

## Pattern Format

All patterns are PostgreSQL-compatible:
- Use `\s` for whitespace (not `\s+` in some cases)
- Use `\d` for digits
- Use `\w` for word characters
- Use `[^\n:]` for "not newline or colon"
- Capture groups `([...])` extract the values
- No `(?i)` flag needed - case-insensitive flag passed separately

## Status

✅ **Migration Created** - `030-update-tender-notice-fields-regex.xml`
✅ **Master Changelog Updated** - Added to `db.changelog-master.xml`
✅ **Ready to Apply** - Will run on next application start

The database will be updated with all Tender Notice field regex patterns when the application starts or when Liquibase runs.

