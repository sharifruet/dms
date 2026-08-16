import React, { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
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
import { Tender } from '../../types/procurement';
import useProcurementRole from '../../hooks/useProcurementRole';

interface Props {
  packageId: number;
  onChanged: () => void;
}

/**
 * Tender attempts for a package (Q-2, REQ-L14).
 *
 * A failed tender is not edited or deleted — re-tendering opens a new attempt under the
 * same package, and the failed one keeps its documents, bidders and BER. This panel is
 * where that history is visible, and where the re-tender is started.
 */
const TenderAttempts: React.FC<Props> = ({ packageId, onChanged }) => {
  const [attempts, setAttempts] = useState<Tender[]>([]);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const { canApprove } = useProcurementRole();

  const load = useCallback(async () => {
    try {
      setAttempts(await procurementService.getTenderAttempts(packageId));
    } catch {
      // A package that has not reached Stage 2 has no attempts yet; that is not an error
      setAttempts([]);
    }
  }, [packageId]);

  useEffect(() => {
    load();
  }, [load]);

  const handleReTender = async () => {
    setBusy(true);
    setError(null);
    try {
      await procurementService.reTender(packageId, reason);
      setDialogOpen(false);
      setReason('');
      await load();
      onChanged();
    } catch (e: any) {
      setError(e?.response?.data?.error || 'Could not re-tender this package');
    } finally {
      setBusy(false);
    }
  };

  // Nothing to show before the first tender exists, and nothing worth showing when there
  // has only ever been one attempt that is still running
  const hasHistory = attempts.length > 1;
  const current = attempts.find((a) => a.isCurrent);

  return (
    <Paper variant="outlined" sx={{ p: 2, mt: 2 }}>
      <Stack direction="row" alignItems="center" sx={{ mb: 1 }}>
        <Typography variant="subtitle2" sx={{ flexGrow: 1 }}>
          Tender attempts
        </Typography>
        {canApprove && current && (
          <Button size="small" color="warning" onClick={() => setDialogOpen(true)}>
            Declare failed and re-tender
          </Button>
        )}
      </Stack>

      {attempts.length === 0 ? (
        <Typography variant="body2" color="text.secondary">
          No tender yet. One is created when the Tender Notice is captured.
        </Typography>
      ) : (
        <>
          {hasHistory && (
            <Alert severity="info" sx={{ mb: 2 }}>
              This package has been re-tendered. Earlier attempts are kept in full — their
              documents, bidders and BER are still readable — but they no longer drive the
              stage gates or the package's live figures.
            </Alert>
          )}
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Attempt</TableCell>
                <TableCell>Type / Method</TableCell>
                <TableCell>Closing</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Why it failed</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {attempts.map((attempt) => (
                <TableRow key={attempt.id} sx={{ opacity: attempt.isCurrent ? 1 : 0.65 }}>
                  <TableCell>#{attempt.attemptNo}</TableCell>
                  <TableCell>
                    {[attempt.procurementType, attempt.procurementMethod]
                      .filter(Boolean)
                      .join(' / ') || '—'}
                  </TableCell>
                  <TableCell>{attempt.closingDate || '—'}</TableCell>
                  <TableCell>
                    {attempt.isCurrent ? (
                      <Chip label="Current" color="primary" size="small" />
                    ) : (
                      <Chip label="Superseded" size="small" />
                    )}
                  </TableCell>
                  <TableCell>{attempt.failureReason || '—'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Declare this tender failed and re-tender</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            A new tender attempt is opened under the same package. The package number, its
            APP linkage and its budget are untouched, and this attempt is kept as history
            with everything captured against it. Stages 2 to 7 reopen against the new
            attempt.
          </Typography>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <TextField
            autoFocus
            fullWidth
            multiline
            minRows={2}
            label="Why did this tender fail? (required)"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            color="warning"
            disabled={!reason.trim() || busy}
            onClick={handleReTender}
          >
            Re-tender
          </Button>
        </DialogActions>
      </Dialog>
    </Paper>
  );
};

export default TenderAttempts;
