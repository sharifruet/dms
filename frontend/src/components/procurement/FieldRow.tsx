import React, { useState } from 'react';
import {
  Box,
  Button,
  Chip,
  IconButton,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import CheckIcon from '@mui/icons-material/Check';
import EditIcon from '@mui/icons-material/Edit';
import HistoryIcon from '@mui/icons-material/History';
import DescriptionIcon from '@mui/icons-material/Description';
import { ExtractedField } from '../../types/procurement';

interface Props {
  field: ExtractedField;
  onVerify: (field: ExtractedField) => void;
  onOverride: (field: ExtractedField, value: string) => void;
  onShowSource?: (field: ExtractedField) => void;
  onShowHistory?: (field: ExtractedField) => void;
}

const displayValue = (field: ExtractedField): string => {
  if (field.textValue) return field.textValue;
  if (field.numericValue !== undefined && field.numericValue !== null) return String(field.numericValue);
  if (field.dateValue) return field.dateValue;
  if (field.boolValue !== undefined && field.boolValue !== null) return field.boolValue ? 'Yes' : 'No';
  return '';
};

const statusChip = (field: ExtractedField) => {
  switch (field.status) {
    case 'VERIFIED':
      return <Chip label="verified" size="small" color="success" variant="outlined" />;
    case 'MANUAL_OVERRIDE':
      return <Chip label="corrected" size="small" color="info" variant="outlined" />;
    case 'REJECTED':
      return <Chip label="rejected" size="small" color="error" variant="outlined" />;
    default:
      return <Chip label="unverified" size="small" color="warning" variant="outlined" />;
  }
};

/**
 * One captured value in the verify screen.
 *
 * Confidence is shown rather than hidden: a user deciding whether to trust a reading
 * needs to know how sure the engine was, and needs the source document one click away.
 */
const FieldRow: React.FC<Props> = ({ field, onVerify, onOverride, onShowSource, onShowHistory }) => {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(displayValue(field));

  const confidence = field.ocrConfidence !== undefined && field.ocrConfidence !== null
    ? Math.round(Number(field.ocrConfidence) * 100)
    : null;

  const lowConfidence = confidence !== null && confidence < 80;
  const notFound = field.validationState === 'NOT_FOUND';

  const save = () => {
    onOverride(field, draft);
    setEditing(false);
  };

  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'flex-start',
        gap: 2,
        py: 1.25,
        px: 1,
        borderBottom: 1,
        borderColor: 'divider',
        backgroundColor: notFound ? 'warning.50' : 'transparent',
      }}
    >
      <Box sx={{ width: 240, flexShrink: 0 }}>
        <Typography variant="body2" sx={{ fontWeight: 500 }}>
          {field.fieldLabel || field.fieldKey}
          {field.isMandatory && <span style={{ color: '#d32f2f' }}> *</span>}
        </Typography>
        <Typography variant="caption" color="text.secondary">
          {field.captureSource === 'MANUAL' ? 'entered by hand' : 'read by OCR'}
          {confidence !== null && ` · ${confidence}% confident`}
        </Typography>
      </Box>

      <Box sx={{ flexGrow: 1, minWidth: 0 }}>
        {editing ? (
          <Stack direction="row" spacing={1}>
            <TextField
              size="small"
              fullWidth
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              autoFocus
              onKeyDown={(e) => {
                if (e.key === 'Enter') save();
                if (e.key === 'Escape') setEditing(false);
              }}
            />
            <Button size="small" variant="contained" onClick={save}>
              Save
            </Button>
            <Button size="small" onClick={() => setEditing(false)}>
              Cancel
            </Button>
          </Stack>
        ) : (
          <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
            <Typography
              variant="body2"
              sx={{ color: notFound ? 'text.disabled' : 'text.primary', fontFamily: 'monospace' }}
            >
              {displayValue(field) || (notFound ? 'not found in document' : '—')}
            </Typography>
            {statusChip(field)}
            {lowConfidence && (
              <Chip label="check this" size="small" color="warning" />
            )}
          </Stack>
        )}

        {/* What the document literally said, when a correction has moved away from it */}
        {field.status === 'MANUAL_OVERRIDE' && field.rawValue && field.rawValue !== displayValue(field) && (
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
            OCR read: <em>{field.rawValue}</em>
          </Typography>
        )}
        {field.validationMessage && (
          <Typography
            variant="caption"
            // A conflict is a stronger signal than a caution: OCR is actively disagreeing
            // with something a person confirmed, and someone should look at the document
            color={field.validationState === 'CONFLICT' ? 'error.main' : 'warning.main'}
            sx={{ display: 'block', mt: 0.5 }}
          >
            {field.validationState === 'CONFLICT' && <strong>Disagreement: </strong>}
            {field.validationMessage}
          </Typography>
        )}
      </Box>

      <Stack direction="row" spacing={0.5} sx={{ flexShrink: 0 }}>
        {field.status === 'OCR_SUGGESTED' && !notFound && (
          <Tooltip title="Confirm this value matches the document">
            <IconButton size="small" color="success" onClick={() => onVerify(field)}>
              <CheckIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        )}
        <Tooltip title="Correct this value">
          <IconButton size="small" onClick={() => { setDraft(displayValue(field)); setEditing(true); }}>
            <EditIcon fontSize="small" />
          </IconButton>
        </Tooltip>
        {field.documentId && onShowSource && (
          <Tooltip title="Show the source document">
            <IconButton size="small" onClick={() => onShowSource(field)}>
              <DescriptionIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        )}
        {onShowHistory && (
          <Tooltip title="Change history">
            <IconButton size="small" onClick={() => onShowHistory(field)}>
              <HistoryIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        )}
      </Stack>
    </Box>
  );
};

export default FieldRow;
