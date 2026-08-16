import React, { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Chip,
  Grid,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { CatalogueField, ExtractedField } from '../../types/procurement';

interface Props {
  catalogue: CatalogueField[];
  fields: ExtractedField[];
  disabled?: boolean;
  onSave: (values: Record<string, string>) => Promise<void>;
}

/**
 * Entering values by hand.
 *
 * Not every field comes from OCR. Stages 5, 8 and 9 are manual end to end, and the OCE
 * at Stage 4 is typed in at BER upload (Q-9) because it exists nowhere else in the
 * system. Without this form those values have no way in at all, and the stages they gate
 * can never be completed.
 *
 * Fields already captured are shown with their current value so this doubles as a
 * correction form; the provenance trail is kept by the server, which records the change
 * rather than overwriting the original reading.
 */
const ManualFieldForm: React.FC<Props> = ({ catalogue, fields, disabled, onSave }) => {
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

  // Manual fields first, and among them the mandatory ones that are still empty - those
  // are what is actually blocking the stage
  const entries = useMemo(() => {
    const manual = catalogue.filter((c) => c.captureSource === 'MANUAL');
    return [...manual].sort((a, b) => {
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

  const missingMandatory = entries.filter((e) => e.isMandatory && !byKey[e.fieldKey]);
  const dirty = Object.values(draft).some((v) => v !== '');

  return (
    <Paper variant="outlined" sx={{ p: 2, mt: 2 }}>
      <Stack direction="row" alignItems="center" sx={{ mb: 1 }}>
        <Typography variant="subtitle2" sx={{ flexGrow: 1 }}>
          Entered by hand
        </Typography>
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
                size="small"
                type={inputType(field.fieldType)}
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
              />
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
