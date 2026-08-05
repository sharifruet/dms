import React, { useCallback, useEffect, useState } from 'react';
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
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import procurementService from '../../services/procurementService';

const daysUntil = (iso?: string): number | null => {
  if (!iso) return null;
  const diff = new Date(iso).getTime() - Date.now();
  return Math.ceil(diff / (1000 * 60 * 60 * 24));
};

/**
 * Every tracked expiry across the estate, soonest first.
 *
 * Extending an instrument supersedes the tracker rather than editing it, so the
 * original expiry stays on the record.
 */
const ExpiryDashboard: React.FC = () => {
  const [rows, setRows] = useState<any[]>([]);
  const [withinDays, setWithinDays] = useState(90);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [target, setTarget] = useState<any | null>(null);
  const [newDate, setNewDate] = useState('');
  const [reason, setReason] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setRows(await procurementService.getExpiries(withinDays));
    } catch (e: any) {
      setError(e?.response?.data?.error || 'Could not load expiries');
    } finally {
      setLoading(false);
    }
  }, [withinDays]);

  useEffect(() => {
    load();
  }, [load]);

  const supersede = async () => {
    try {
      await procurementService.supersedeExpiry(target.id, newDate, reason);
      setTarget(null);
      setNewDate('');
      setReason('');
      await load();
    } catch (e: any) {
      setError(e?.response?.data?.error || 'Could not extend this instrument');
    }
  };

  const urgency = (days: number | null) => {
    if (days === null) return 'default' as const;
    if (days < 0) return 'error' as const;
    if (days <= 15) return 'error' as const;
    if (days <= 30) return 'warning' as const;
    return 'default' as const;
  };

  return (
    <Box sx={{ p: 3 }}>
      <Stack direction="row" alignItems="center" spacing={2} sx={{ mb: 2 }}>
        <Typography variant="h5" sx={{ flexGrow: 1 }}>Expiries</Typography>
        <TextField
          select size="small" label="Window" sx={{ minWidth: 160 }}
          value={withinDays} onChange={(e) => setWithinDays(Number(e.target.value))}
        >
          <MenuItem value={15}>Next 15 days</MenuItem>
          <MenuItem value={30}>Next 30 days</MenuItem>
          <MenuItem value={60}>Next 60 days</MenuItem>
          <MenuItem value={90}>Next 90 days</MenuItem>
          <MenuItem value={365}>Next year</MenuItem>
        </TextField>
      </Stack>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}

      <Paper variant="outlined">
        {loading && <LinearProgress />}
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Instrument</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Expires</TableCell>
              <TableCell>Days left</TableCell>
              <TableCell>Notes</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((row) => {
              const days = daysUntil(row.expiryDate);
              return (
                <TableRow key={row.id}>
                  <TableCell>{row.entityType || '—'}</TableCell>
                  <TableCell>{row.expiryType}</TableCell>
                  <TableCell>{row.expiryDate?.substring(0, 10)}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      color={urgency(days)}
                      label={days === null ? '—' : days < 0 ? `${Math.abs(days)} overdue` : days}
                    />
                  </TableCell>
                  <TableCell>{row.notes}</TableCell>
                  <TableCell align="right">
                    <Button size="small" onClick={() => setTarget(row)}>Extend</Button>
                  </TableCell>
                </TableRow>
              );
            })}
            {!loading && rows.length === 0 && (
              <TableRow>
                <TableCell colSpan={6}>
                  <Box sx={{ py: 4, textAlign: 'center' }}>
                    <Typography variant="body2" color="text.secondary">
                      Nothing expiring in this window.
                    </Typography>
                  </Box>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>

      <Dialog open={target !== null} onClose={() => setTarget(null)} fullWidth maxWidth="sm">
        <DialogTitle>Extend this instrument</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            The current tracker is kept and marked as superseded, so the original expiry
            stays on the record.
          </Typography>
          <Stack spacing={2}>
            <TextField
              type="date" label="New expiry date" InputLabelProps={{ shrink: true }}
              value={newDate} onChange={(e) => setNewDate(e.target.value)} fullWidth
            />
            <TextField
              label="Reason" value={reason} onChange={(e) => setReason(e.target.value)} fullWidth
              placeholder="e.g. LC amendment 1"
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTarget(null)}>Cancel</Button>
          <Button variant="contained" disabled={!newDate} onClick={supersede}>Extend</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default ExpiryDashboard;
