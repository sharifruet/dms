import React, { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  IconButton,
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
import DeleteIcon from '@mui/icons-material/DeleteOutline';
import procurementService from '../../services/procurementService';
import { PriceScheduleLine } from '../../types/procurement';

interface Props {
  packageId: number;
  disabled?: boolean;
  onChanged?: () => void;
}

const num = (value?: number) =>
  value === undefined || value === null || Number.isNaN(value)
    ? '—'
    : Number(value).toLocaleString(undefined, { maximumFractionDigits: 2 });

/**
 * The e-GP price schedule — the item and price baseline for Stages 12 and 13 (REQ-10.3).
 *
 * <p>Entered as a grid because that is the shape of the source document, and because
 * pasting from a spreadsheet is how anybody sane transfers twenty line items. The lines
 * matter downstream: a delivery names the line it fulfils and is checked against the
 * quantity here, and the billed total is checked against what these lines are worth.
 *
 * <p>Nothing here blocks the stage. A services contract may have no itemisation at all,
 * and refusing to progress for want of a table that does not exist would be a rule
 * inventing its own paperwork.
 */
const PriceScheduleTable: React.FC<Props> = ({ packageId, disabled, onChanged }) => {
  const [lines, setLines] = useState<PriceScheduleLine[]>([]);
  const [source, setSource] = useState('');
  const [deliveryPeriodDays, setDeliveryPeriodDays] = useState<string>('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    let cancelled = false;
    procurementService
      .getPriceSchedule(packageId)
      .then((data) => {
        if (cancelled) return;
        setLines(data?.lines || []);
        setSource(data?.schedule?.source || '');
        setDeliveryPeriodDays(
          data?.schedule?.deliveryPeriodDays ? String(data.schedule.deliveryPeriodDays) : '',
        );
      })
      .catch(() => {
        if (!cancelled) setError('Could not load the price schedule');
      });
    return () => {
      cancelled = true;
    };
  }, [packageId]);

  const update = (index: number, patch: Partial<PriceScheduleLine>) => {
    setLines(lines.map((line, i) => (i === index ? { ...line, ...patch } : line)));
    setSaved(false);
  };

  const addLine = () => {
    setLines([...lines, { lineNo: lines.length + 1, currency: 'BDT' }]);
    setSaved(false);
  };

  const removeLine = (index: number) => {
    setLines(lines.filter((_, i) => i !== index));
    setSaved(false);
  };

  const save = async () => {
    setSaving(true);
    setError(null);
    try {
      await procurementService.savePriceSchedule(packageId, {
        source: source || undefined,
        deliveryPeriodDays: deliveryPeriodDays ? Number(deliveryPeriodDays) : undefined,
        lines,
      });
      setSaved(true);
      onChanged?.();
    } catch (e: any) {
      setError(e?.response?.data?.error || 'Could not save the price schedule');
    } finally {
      setSaving(false);
    }
  };

  // Shown rather than stored: the server computes it too, and a total that disagrees with
  // the lines on screen is the first sign somebody mistyped a quantity
  const total = lines.reduce((sum, line) => {
    const amount = line.lineAmount
      ?? (line.quantity ?? 0) * (line.unitPrice ?? 0);
    return sum + (Number.isFinite(amount) ? Number(amount) : 0);
  }, 0);

  return (
    <Paper variant="outlined" sx={{ p: 2, mt: 2 }}>
      <Stack direction="row" alignItems="center" spacing={2} sx={{ mb: 1 }}>
        <Typography variant="subtitle2" sx={{ flexGrow: 1 }}>
          e-GP price schedule
        </Typography>
        <Typography variant="caption" color="text.secondary">
          Baseline total {num(total)}
        </Typography>
      </Stack>
      <Typography variant="caption" color="text.secondary">
        Deliveries and invoices are measured against these lines (REQ-10.3).
      </Typography>

      {error && <Alert severity="error" sx={{ my: 2 }}>{error}</Alert>}
      {saved && <Alert severity="success" sx={{ my: 2 }} onClose={() => setSaved(false)}>Saved.</Alert>}

      <Stack direction="row" spacing={1} sx={{ my: 2 }}>
        <TextField
          size="small" label="Source" placeholder="e-GP" sx={{ minWidth: 160 }}
          value={source} disabled={disabled} onChange={(e) => setSource(e.target.value)}
        />
        <TextField
          size="small" type="number" label="Delivery period (days)" sx={{ minWidth: 200 }}
          helperText="Overrides the contract's period for the delivery window"
          value={deliveryPeriodDays} disabled={disabled}
          onChange={(e) => setDeliveryPeriodDays(e.target.value)}
        />
      </Stack>

      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell sx={{ width: 60 }}>#</TableCell>
            <TableCell>Item</TableCell>
            <TableCell>Description</TableCell>
            <TableCell align="right">Qty</TableCell>
            <TableCell>UoM</TableCell>
            <TableCell align="right">Unit price</TableCell>
            <TableCell align="right">Amount</TableCell>
            <TableCell />
          </TableRow>
        </TableHead>
        <TableBody>
          {lines.map((line, index) => (
            <TableRow key={line.id ?? `new-${index}`}>
              <TableCell>{line.lineNo ?? index + 1}</TableCell>
              <TableCell>
                <TextField
                  variant="standard" size="small" value={line.itemCode ?? ''} disabled={disabled}
                  onChange={(e) => update(index, { itemCode: e.target.value })}
                />
              </TableCell>
              <TableCell>
                <TextField
                  variant="standard" size="small" fullWidth
                  value={line.itemDescription ?? ''} disabled={disabled}
                  onChange={(e) => update(index, { itemDescription: e.target.value })}
                />
              </TableCell>
              <TableCell align="right">
                <TextField
                  variant="standard" size="small" type="number" sx={{ width: 80 }}
                  value={line.quantity ?? ''} disabled={disabled}
                  onChange={(e) => update(index, { quantity: Number(e.target.value) })}
                />
              </TableCell>
              <TableCell>
                <TextField
                  variant="standard" size="small" sx={{ width: 70 }}
                  value={line.uom ?? ''} disabled={disabled}
                  onChange={(e) => update(index, { uom: e.target.value })}
                />
              </TableCell>
              <TableCell align="right">
                <TextField
                  variant="standard" size="small" type="number" sx={{ width: 110 }}
                  value={line.unitPrice ?? ''} disabled={disabled}
                  onChange={(e) => update(index, { unitPrice: Number(e.target.value) })}
                />
              </TableCell>
              <TableCell align="right">
                {num(line.lineAmount ?? (line.quantity ?? 0) * (line.unitPrice ?? 0))}
              </TableCell>
              <TableCell align="right">
                <IconButton size="small" disabled={disabled} onClick={() => removeLine(index)}>
                  <DeleteIcon fontSize="small" />
                </IconButton>
              </TableCell>
            </TableRow>
          ))}
          {lines.length === 0 && (
            <TableRow>
              <TableCell colSpan={8}>
                <Box sx={{ py: 2 }}>
                  <Typography variant="body2" color="text.secondary">
                    No schedule filed. Without one, deliveries and invoices have no item
                    baseline to be checked against.
                  </Typography>
                </Box>
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>

      <Stack direction="row" spacing={1} justifyContent="flex-end" sx={{ mt: 2 }}>
        <Button size="small" onClick={addLine} disabled={disabled}>Add line</Button>
        <Button size="small" variant="contained" onClick={save} disabled={disabled || saving}>
          {saving ? 'Saving…' : 'Save schedule'}
        </Button>
      </Stack>
    </Paper>
  );
};

export default PriceScheduleTable;
