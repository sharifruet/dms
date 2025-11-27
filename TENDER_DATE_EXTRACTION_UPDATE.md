# Tender Notice Date Field Extraction Update

## Overview

Updated the Tender Notice metadata extraction to correctly identify and extract:
- **tenderId**: `1156325`
- **tenderDate**: `24-Sep-2025 13:00` (Publication/Scheduled Date)
- **closingDate**: `22-Oct-2025 14:00` (Tender Closing Date)

## Changes Made

### 1. Updated Date Field Mapping

**Before:**
- `tenderDate` was extracted from general date patterns (fallback)
- `closingDate` and `publicationDate` were separate fields

**After:**
- `tenderDate` = Publication Date (when tender was published/issued)
  - Extracted from: `"Scheduled Tender/Proposal Document last selling / downloading Date and Time : 24-Sep-2025 13:00"`
  - Normalized to: `"2025-09-24 13:00"`

- `closingDate` = Tender Closing Date (when tender closes)
  - Extracted from: `"Tender/Proposal Closing Date and Time : 22-Oct-2025 14:00"`
  - Normalized to: `"2025-10-22 14:00"`

### 2. Enhanced Extraction Logic

**File**: `backend/src/main/java/com/bpdb/dms/service/DocumentMetadataService.java`

#### Updated Pattern (Line ~78)
```java
// Matches: "Tender/Proposal ID : 1156325"
private static final Pattern TENDER_ID_PATTERN = Pattern.compile(
    "(?i)(?:tender|proposal)\\s*/\\s*(?:tender|proposal)?\\s*(?:id|number|no\\.?|#)\\s*[:\\-]?\\s*([0-9]+)"
);
```

#### Updated Publication Date Pattern (Line ~113-115)
```java
// Matches: "Scheduled Tender/Proposal Document last selling / downloading Date and Time : 24-Sep-2025 13:00"
private static final Pattern PUBLICATION_DATE_TIME_PATTERN = Pattern.compile(
    "(?i)(?:scheduled\\s+)?(?:tender|proposal)\\s*(?:document\\s+)?(?:publication|last\\s+selling\\s*/\\s*downloading|last\\s+selling|downloading)\\s*(?:date\\s+and\\s+time|date)?\\s*[:\\-]?\\s*(\\d{1,2}[\\-/]\\w+[\\-/]\\d{4}\\s+\\d{1,2}:\\d{2})"
);
```

#### Updated Extraction Logic (Lines ~235-270)

1. **Extract Publication Date First**:
   ```java
   String publicationDate = null;
   if (!inferred.containsKey("publicationDate")) {
       Optional<String> pubDate = extractFirstMatch(PUBLICATION_DATE_TIME_PATTERN, extractedText, 1)
           .map(this::normalizeDateTime);
       if (pubDate.isPresent()) {
           publicationDate = pubDate.get();
           inferred.put("publicationDate", publicationDate);
       }
   }
   ```

2. **Extract Closing Date**:
   ```java
   if (!inferred.containsKey("closingDate") && !inferred.containsKey("expiryDate")) {
       extractFirstMatch(CLOSING_DATE_TIME_PATTERN, extractedText, 1)
           .map(this::normalizeDateTime)
           .ifPresent(value -> inferred.put("closingDate", value));
   }
   ```

3. **Set tenderDate = publicationDate**:
   ```java
   if (!inferred.containsKey("tenderDate") && !inferred.containsKey("date")) {
       if (publicationDate != null) {
           inferred.put("tenderDate", publicationDate);
       } else {
           // Fallback: extract first date found
           extractDate(extractedText).ifPresent(date -> inferred.put("tenderDate", date));
       }
   }
   ```

## Sample Data Mapping

Based on the provided Tender Notice OCR text:

| OCR Text | Field | Value |
|----------|-------|-------|
| `Tender/Proposal ID : 1156325` | `tenderId` | `1156325` |
| `Scheduled Tender/Proposal Document last selling / downloading Date and Time : 24-Sep-2025 13:00` | `tenderDate` | `2025-09-24 13:00` |
| `Scheduled Tender/Proposal Document last selling / downloading Date and Time : 24-Sep-2025 13:00` | `publicationDate` | `2025-09-24 13:00` |
| `Tender/Proposal Closing Date and Time : 22-Oct-2025 14:00` | `closingDate` | `2025-10-22 14:00` |

## Date-Time Normalization

The `normalizeDateTime()` method converts OCR date-time strings to standardized format:
- Input: `"24-Sep-2025 13:00"`
- Output: `"2025-09-24 13:00"`

This ensures consistent date formatting across the system.

## Field Priority

1. **Publication Date** → Used for both `publicationDate` and `tenderDate`
2. **Closing Date** → Used for `closingDate` (also maps to `expiryDate` if needed)
3. **Opening Date** → Used for `openingDate`

## Testing

To verify the extraction is working correctly:

1. **Upload a Tender Notice PDF** with the format shown in the sample data
2. **Wait for OCR processing** (check application logs)
3. **Check document metadata**:
   ```sql
   SELECT key, value 
   FROM document_metadata 
   WHERE document_id = {your_document_id}
   AND key IN ('tenderId', 'tenderDate', 'closingDate', 'publicationDate')
   ORDER BY key;
   ```

4. **Expected results**:
   - `tenderId`: `"1156325"`
   - `tenderDate`: `"2025-09-24 13:00"`
   - `closingDate`: `"2025-10-22 14:00"`
   - `publicationDate`: `"2025-09-24 13:00"`

## Status

✅ **Updated** - tenderId extraction pattern
✅ **Updated** - tenderDate now uses publication date
✅ **Updated** - closingDate correctly extracted
✅ **Tested** - Patterns match sample data format
✅ **Ready** - Code changes complete and compiled

The system now correctly identifies:
- **tenderId** = `1156325`
- **tenderDate** = Publication date (`24-Sep-2025 13:00`)
- **closingDate** = Closing date (`22-Oct-2025 14:00`)

