import React, { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Chip,
  Grid,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { CatalogueField, ExtractedField, MasterListValue } from '../../types/procurement';

/**
 * Catalogue field keys constrained to a master list (Q-8, REQ-2.4), mapped to the list
 * that governs them. Keyed rather than inferred, because "type" appears in field names
 * that have nothing to do with procurement type.
 */
const MASTER_LIST_FIELDS: Record<string, string> = {
  procurement_type: 'PROCUREMENT_TYPE',
  procurement_method: 'PROCUREMENT_METHOD',
  procurement_nature: 'PROCUREMENT_NATURE',
};

/** Jackson on ProcurementMasterList emits valueCode / valueLabel, not code / label. */
const optionCode = (option: MasterListValue): string =>
  option.code || option.valueCode || '';
const optionLabel = (option: MasterListValue): string =>
  option.label || option.valueLabel || optionCode(option);

interface Props {
  catalogue: CatalogueField[];
  fields: ExtractedField[];
  disabled?: boolean;
  /** Permitted values for the fields governed by a master list (Q-8, REQ-2.4). */
  masterLists?: Record<string, MasterListValue[]>;
  onSave: (values: Record<string, string>) => Promise<void>;
}

/**
 * Catalogue entity types that are repeating rows (one bidder, one delivery, …).
 * Those are created by their own tables, not this form — saving a single
 * "Bidder Name" here would tick Gate 3 without a BER_BIDDER row (StageDataService).
 * Keep in step with StageDefinitionService.isRepeatingEntity.
 */
const REPEATING_ENTITY_TYPES = new Set([
  'BER_BIDDER',
  'DELIVERY',
  'INVOICE',
  'PAYMENT',
  'INSPECTION_EVENT',
]);

/**
 * Entering values by hand.
 *
 * Document upload is optional on every stage, so OCR-sourced catalogue fields must
 * still be typeable here — otherwise Stage 3's Number of Bidders (and the rest) only
 * appear after a file is filed, and Complete stays blocked on "(not captured)".
 * Stages 5, 8 and 9 plus the OCE at Stage 4 (Q-9) were already manual end to end.
 *
 * Repeating rows (bidders, deliveries, invoices) stay on their own tables —
 * keep REPEATING_ENTITY_TYPES in step with StageDefinitionService.isRepeatingEntity.
 *
 * Already-captured MANUAL fields stay on this form as a correction; OCR readings
 * that have a row move to FieldRow instead. The provenance trail is kept by the
 * server, which records the change rather than overwriting the original reading.
 */
const ManualFieldForm: React.FC<Props> = ({
  catalogue,
  fields,
  disabled,
  masterLists = {},
  onSave,
}) => {
  const [draft, setDraft] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  const byKey = useMemo(() => {
    const map: Record<string, ExtractedField> = {};
    fields.forEach((f) => {
      map[f.fieldKey] = f;
    });
    return map;
  }, [fields]);

  /** What a captured field currently holds, as text for the input. */
  const currentValue = (field: CatalogueField): string => {
    const captured = byKey[field.fieldKey];
    if (!captured) {
      return '';
    }
    if (captured.textValue) return captured.textValue;
    if (captured.numericValue !== undefined && captured.numericValue !== null) {
      return String(captured.numericValue);
    }
    if (captured.dateValue) return captured.dateValue;
    if (captured.boolValue !== undefined && captured.boolValue !== null) {
      return String(captured.boolValue);
    }
    return captured.rawValue ?? '';
  };

  // Every non-repeating catalogue field that still needs a value, plus MANUAL fields
  // already captured (those double as a correction form). Uncaptured OCR fields are
  // included because the matching document is optional.
  const entries = useMemo(() => {
    const fillable = catalogue.filter((c) => {
      if (c.entityType && REPEATING_ENTITY_TYPES.has(c.entityType)) {
        return false;
      }
      if (c.captureSource === 'MANUAL') {
        return true;
      }
      return !byKey[c.fieldKey];
    });
    return [...fillable].sort((a, b) => {
      const aMissing = a.isMandatory && !byKey[a.fieldKey] ? 0 : 1;
      const bMissing = b.isMandatory && !byKey[b.fieldKey] ? 0 : 1;
      if (aMissing !== bMissing) return aMissing - bMissing;
      return (a.displayOrder ?? 0) - (b.displayOrder ?? 0);
    });
  }, [catalogue, byKey]);

  if (entries.length === 0) {
    return null;
  }

  const inputType = (fieldType: string): string => {
    switch (fieldType?.toUpperCase()) {
      case 'DATE':
        return 'date';
      case 'NUMBER':
      case 'CURRENCY':
        return 'number';
      default:
        return 'text';
    }
  };

  const handleSave = async () => {
    const values = Object.fromEntries(
      Object.entries(draft).filter(([, v]) => v !== undefined && v !== ''),
    );
    if (Object.keys(values).length === 0) {
      return;
    }
    setSaving(true);
    setError(null);
    setSaved(false);
    try {
      await onSave(values);
      setDraft({});
      setSaved(true);
    } catch (e: any) {
      setError(e?.response?.data?.error || 'Could not save these values');
    } finally {
      setSaving(false);
    }
  };

  /**
   * The permitted values for a field, or none when it is free text.
   *
   * An unmatched value is only a warning on the server, so offering the list here is
   * about not making somebody guess whether the system wants "OTM" or "Open Tender
   * Method" — the mismatch it prevents is a spelling, not a fraud.
   */
  const optionsFor = (field: CatalogueField): MasterListValue[] => {
    const listKey = MASTER_LIST_FIELDS[field.fieldKey];
    return listKey ? masterLists[listKey] ?? [] : [];
  };

  const missingMandatory = entries.filter((e) => e.isMandatory && !byKey[e.fieldKey]);
  const dirty = Object.values(draft).some((v) => v !== '');

  return (
    <Paper variant="outlined" sx={{ p: 2, mt: 2 }}>
      <Stack direction="row" alignItems="flex-start" sx={{ mb: 1 }} spacing={1}>
        <Stack sx={{ flexGrow: 1 }}>
          <Typography variant="subtitle2">Stage fields</Typography>
          <Typography variant="caption" color="text.secondary">
            Uploading a document is optional — enter values here, or extract them from a file.
          </Typography>
        </Stack>
        {missingMandatory.length > 0 && (
          <Chip
            size="small"
            color="warning"
            label={`${missingMandatory.length} still needed`}
          />
        )}
      </Stack>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {saved && !dirty && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSaved(false)}>
          Saved.
        </Alert>
      )}

      <Grid container spacing={2}>
        {entries.map((field) => {
          const captured = byKey[field.fieldKey];
          const missing = field.isMandatory && !captured;
          return (
            <Grid item xs={12} sm={6} key={field.fieldKey}>
              <TextField
                fullWidth
                select={optionsFor(field).length > 0}
                size="small"
                type={optionsFor(field).length > 0 ? undefined : inputType(field.fieldType)}
                label={field.fieldLabel + (field.isMandatory ? ' *' : '')}
                placeholder={captured ? undefined : 'Not captured yet'}
                value={draft[field.fieldKey] ?? currentValue(field)}
                disabled={disabled || saving}
                error={missing && !(draft[field.fieldKey] ?? '')}
                helperText={
                  missing && !(draft[field.fieldKey] ?? '')
                    ? 'Required before this stage can be completed'
                    : undefined
                }
                InputLabelProps={
                  inputType(field.fieldType) === 'date' ? { shrink: true } : undefined
                }
                onChange={(e) =>
                  setDraft({ ...draft, [field.fieldKey]: e.target.value })
                }
              >
                {optionsFor(field).map((option) => {
                  const code = optionCode(option);
                  return (
                    <MenuItem key={code} value={code}>
                      {optionLabel(option)}
                    </MenuItem>
                  );
                })}
              </TextField>
            </Grid>
          );
        })}
      </Grid>

      <Stack direction="row" justifyContent="flex-end" sx={{ mt: 2 }}>
        <Button
          variant="contained"
          size="small"
          disabled={disabled || saving || !dirty}
          onClick={handleSave}
        >
          Save values
        </Button>
      </Stack>
    </Paper>
  );
};

export default ManualFieldForm;
