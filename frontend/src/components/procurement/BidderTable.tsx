import React, { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  IconButton,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import ContentPasteIcon from '@mui/icons-material/ContentPaste';
import { BerBidder } from '../../types/procurement';

interface Props {
  bidders: BerBidder[];
  onSave: (bidders: BerBidder[]) => Promise<void>;
}

const emptyRow = (): BerBidder => ({
  bidderName: '',
  biddingPrice: undefined,
  currency: 'BDT',
  isResponsive: true,
  deviationPct: undefined,
  isAwarded: false,
});

/**
 * The BER bidder table - the single source of bidder data in the system.
 *
 * BER layouts vary too much for table OCR to be dependable, so this is built as a
 * spreadsheet-style grid first: type the rows, or paste them straight out of the BER.
 * Anything OCR manages to pre-fill is a bonus, not the mechanism.
 */
const BidderTable: React.FC<Props> = ({ bidders, onSave }) => {
  const [rows, setRows] = useState<BerBidder[]>(bidders.length ? bidders : [emptyRow()]);
  const [pasteMode, setPasteMode] = useState(false);
  const [pasteText, setPasteText] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (bidders.length) setRows(bidders);
  }, [bidders]);

  const update = (index: number, patch: Partial<BerBidder>) => {
    setRows((prev) => prev.map((r, i) => (i === index ? { ...r, ...patch } : r)));
  };

  const setAwarded = (index: number) => {
    // exactly one awarded bidder carries through to Contract Approval
    setRows((prev) => prev.map((r, i) => ({ ...r, isAwarded: i === index })));
  };

  const addRow = () => setRows((prev) => [...prev, emptyRow()]);
  const removeRow = (index: number) => setRows((prev) => prev.filter((_, i) => i !== index));

  /** Accept a block pasted from the BER: name, price, responsive, deviation. */
  const applyPaste = () => {
    const parsed: BerBidder[] = pasteText
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter(Boolean)
      .map((line) => {
        const cells = line.split(/\t|\s{2,}|,/).map((c) => c.trim()).filter(Boolean);
        const row = emptyRow();
        row.bidderName = cells[0] || '';
        if (cells[1]) {
          const price = Number(cells[1].replace(/[^0-9.\-]/g, ''));
          row.biddingPrice = Number.isNaN(price) ? undefined : price;
        }
        if (cells[2]) {
          row.isResponsive = /^(y|yes|true|responsive)/i.test(cells[2]);
        }
        if (cells[3]) {
          const dev = Number(cells[3].replace(/[^0-9.\-]/g, ''));
          row.deviationPct = Number.isNaN(dev) ? undefined : dev;
        }
        return row;
      });
    if (parsed.length) {
      setRows(parsed);
      setPasteMode(false);
      setPasteText('');
    }
  };

  const save = async () => {
    const valid = rows.filter((r) => r.bidderName.trim());
    if (!valid.length) {
      setError('Add at least one bidder before saving');
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await onSave(valid);
    } catch (e: any) {
      setError(e?.response?.data?.error || 'Could not save the bidder table');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Paper variant="outlined" sx={{ p: 2, mt: 2 }}>
      <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 1 }}>
        <Typography variant="subtitle2" sx={{ flexGrow: 1 }}>
          Bidders (from the BER)
        </Typography>
        <Tooltip title="Paste rows copied from the BER">
          <Button size="small" startIcon={<ContentPasteIcon />} onClick={() => setPasteMode((p) => !p)}>
            Paste
          </Button>
        </Tooltip>
        <Button size="small" startIcon={<AddIcon />} onClick={addRow}>
          Add row
        </Button>
        <Button size="small" variant="contained" onClick={save} disabled={saving}>
          Save bidders
        </Button>
      </Stack>

      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
        The BER is the only place bidder data is held. Bid opening records the count only.
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 1 }}>{error}</Alert>}

      {pasteMode && (
        <Box sx={{ mb: 2 }}>
          <TextField
            multiline
            minRows={4}
            fullWidth
            size="small"
            placeholder={'Bidder name\tPrice\tResponsive\tDeviation %\nABC Ltd\t4320\tyes\t-4.0'}
            value={pasteText}
            onChange={(e) => setPasteText(e.target.value)}
          />
          <Button size="small" variant="contained" sx={{ mt: 1 }} onClick={applyPaste}>
            Use these rows
          </Button>
        </Box>
      )}

      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Bidder name</TableCell>
            <TableCell align="right">Bidding price</TableCell>
            <TableCell align="center">Responsive</TableCell>
            <TableCell align="right">Deviation % vs OCE</TableCell>
            <TableCell align="center">Awarded</TableCell>
            <TableCell />
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((row, index) => (
            <TableRow key={index}>
              <TableCell>
                <TextField
                  variant="standard"
                  fullWidth
                  value={row.bidderName}
                  onChange={(e) => update(index, { bidderName: e.target.value })}
                />
              </TableCell>
              <TableCell align="right">
                <TextField
                  variant="standard"
                  type="number"
                  value={row.biddingPrice ?? ''}
                  onChange={(e) =>
                    update(index, {
                      biddingPrice: e.target.value === '' ? undefined : Number(e.target.value),
                    })
                  }
                  inputProps={{ style: { textAlign: 'right' } }}
                />
              </TableCell>
              <TableCell align="center">
                <Checkbox
                  size="small"
                  checked={!!row.isResponsive}
                  onChange={(e) => update(index, { isResponsive: e.target.checked })}
                />
              </TableCell>
              <TableCell align="right">
                <TextField
                  variant="standard"
                  type="number"
                  placeholder="auto"
                  value={row.deviationPct ?? ''}
                  onChange={(e) =>
                    update(index, {
                      deviationPct: e.target.value === '' ? undefined : Number(e.target.value),
                    })
                  }
                  inputProps={{ style: { textAlign: 'right' } }}
                />
              </TableCell>
              <TableCell align="center">
                <Checkbox
                  size="small"
                  checked={!!row.isAwarded}
                  disabled={!row.isResponsive}
                  onChange={() => setAwarded(index)}
                />
              </TableCell>
              <TableCell align="right">
                <IconButton size="small" onClick={() => removeRow(index)}>
                  <DeleteIcon fontSize="small" />
                </IconButton>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
        Leave Deviation % blank to have it computed from the OCE on the evaluation.
        Enter a percentage (for example -4.0), not a Taka amount.
      </Typography>
    </Paper>
  );
};

export default BidderTable;
