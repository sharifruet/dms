import React, { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  LinearProgress,
  Stack,
  Typography,
} from '@mui/material';
import procurementService from '../../services/procurementService';
import { ExtractedField } from '../../types/procurement';

interface Props {
  field: ExtractedField | null;
  onClose: () => void;
}

/**
 * Where a value came from (REQ-X3, supported by the bbox and page persisted per REQ-P6).
 *
 * <p>The point of a verify screen is that confirming a reading is quicker than re-typing
 * it. That only holds if the user can see the source without hunting: this shows the page
 * the value was read from and the surrounding text with the value marked, so the check is
 * a glance rather than an investigation.
 *
 * <p>It deliberately shows the OCR text rather than drawing a box on the scan. The
 * coordinates are recorded against the image the engine saw — a PDF page rendered at 300
 * DPI, or a pre-processed photograph — and the browser renders something else at a
 * different scale. A box drawn a centimetre off is worse than no box, because it invites
 * somebody to confirm the wrong line. The coordinates are shown as a fact and used to
 * order the text, not to fake a precision the data does not carry.
 */
const SourcePreview: React.FC<Props> = ({ field, onClose }) => {
  const [pages, setPages] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!field?.documentId) {
      setPages([]);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    procurementService
      .getOcr(field.documentId)
      .then((data) => {
        if (!cancelled) setPages(data?.pages || []);
      })
      .catch(() => {
        if (!cancelled) setError('The OCR text for this document is no longer available.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [field]);

  if (!field) {
    return null;
  }

  const value = field.rawValue || field.textValue || '';
  const page = field.pageNo
    ? pages.find((p) => p.pageNo === field.pageNo)
    : pages[0];

  /** The value marked inside the page text, so the eye lands on it. */
  const marked = (text: string) => {
    if (!value || !text) {
      return text;
    }
    const index = text.toLowerCase().indexOf(value.toLowerCase());
    if (index < 0) {
      return text;
    }
    return (
      <>
        {text.slice(0, index)}
        <Box component="mark" sx={{ backgroundColor: '#fff59d', px: 0.5 }}>
          {text.slice(index, index + value.length)}
        </Box>
        {text.slice(index + value.length)}
      </>
    );
  };

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="md">
      <DialogTitle>Where this value came from</DialogTitle>
      <DialogContent dividers>
        <Stack direction="row" spacing={1} sx={{ mb: 2 }} flexWrap="wrap">
          <Chip size="small" label={field.fieldLabel || field.fieldKey} />
          <Chip size="small" variant="outlined" label={`Source: ${field.captureSource}`} />
          {field.pageNo && <Chip size="small" variant="outlined" label={`Page ${field.pageNo}`} />}
          {field.ocrConfidence !== undefined && field.ocrConfidence !== null && (
            <Chip
              size="small"
              variant="outlined"
              label={`Confidence ${Math.round(Number(field.ocrConfidence) * 100)}%`}
            />
          )}
          {field.bbox && (
            <Chip size="small" variant="outlined" label={`Region ${field.bbox}`} />
          )}
        </Stack>

        {field.captureSource !== 'OCR' && (
          <Alert severity="info" sx={{ mb: 2 }}>
            This value was {field.captureSource === 'IMPORT' ? 'imported' : 'entered'} rather
            than read from a scan, so there is no region on a page to point at. The history
            records where it came from.
          </Alert>
        )}

        {loading && <LinearProgress />}
        {error && <Alert severity="warning">{error}</Alert>}

        {!loading && !error && page && (
          <Box
            component="pre"
            sx={{
              whiteSpace: 'pre-wrap',
              fontFamily: 'ui-monospace, monospace',
              fontSize: '0.8rem',
              maxHeight: 400,
              overflow: 'auto',
              backgroundColor: '#fafafa',
              p: 2,
              m: 0,
              border: '1px solid #e0e0e0',
              borderRadius: 1,
            }}
          >
            {marked(page.pageText || '')}
          </Box>
        )}

        {!loading && !error && !page && (
          <Typography variant="body2" color="text.secondary">
            No OCR text is stored for this document. If it was a scan, re-reading it may
            produce some; otherwise the value stands on manual entry alone.
          </Typography>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
};

export default SourcePreview;
