import React, { useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  LinearProgress,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import procurementService from '../../services/procurementService';
import { ExtractedField, ExtractedFieldHistory } from '../../types/procurement';

interface Props {
  field: ExtractedField | null;
  onClose: () => void;
}

/**
 * Every change a value has been through (REQ-P5).
 *
 * <p>The history table has been written to since capture was built, and there was no way
 * to read it. That matters for the thing the design rests on: a value is trustworthy
 * because you can see who confirmed it and what it said before they did.
 */
const FieldHistoryDialog: React.FC<Props> = ({ field, onClose }) => {
  const [rows, setRows] = useState<ExtractedFieldHistory[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!field?.id) {
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    procurementService
      .fieldHistory(field.id)
      .then((data) => {
        if (!cancelled) setRows(data || []);
      })
      .catch(() => {
        if (!cancelled) setError('Could not load the history for this field.');
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

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="md">
      <DialogTitle>{field.fieldLabel || field.fieldKey} — history</DialogTitle>
      <DialogContent dividers>
        {loading && <LinearProgress />}
        {error && <Alert severity="warning">{error}</Alert>}
        {!loading && !error && rows.length === 0 && (
          <Typography variant="body2" color="text.secondary">
            No changes recorded — this value is as it was first captured.
          </Typography>
        )}
        {rows.length > 0 && (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>When</TableCell>
                <TableCell>Was</TableCell>
                <TableCell>Became</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Reason</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.id}>
                  <TableCell>{row.changedAt?.replace('T', ' ').substring(0, 16)}</TableCell>
                  <TableCell>{row.oldValue ?? '—'}</TableCell>
                  <TableCell>{row.newValue ?? '—'}</TableCell>
                  <TableCell>
                    {row.oldStatus ? `${row.oldStatus} → ${row.newStatus}` : row.newStatus}
                  </TableCell>
                  <TableCell>{row.changeReason ?? '—'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
};

export default FieldHistoryDialog;
