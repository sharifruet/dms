import React, { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  FormControl,
  InputLabel,
  ListItemText,
  MenuItem,
  OutlinedInput,
  Paper,
  Select,
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
import { StageDetail } from '../../types/procurement';

interface Props {
  packageId: number;
  stageCode: number;
  detail: StageDetail;
  onChanged: () => Promise<void> | void;
}

/**
 * The stages that hold lists rather than a single form: inspections, deliveries,
 * invoices and payments.
 *
 * Invoices pick the deliveries they bill and payments pick the invoices they settle,
 * because partial delivery and part payment are the normal case, not the exception.
 */
const StageRecords: React.FC<Props> = ({ packageId, stageCode, detail, onChanged }) => {
  const [error, setError] = useState<string | null>(null);
  const [draft, setDraft] = useState<Record<string, any>>({});
  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  const set = (key: string, value: any) => setDraft((prev) => ({ ...prev, [key]: value }));

  const submit = async (fn: () => Promise<any>) => {
    setError(null);
    try {
      await fn();
      setDraft({});
      setSelectedIds([]);
      await onChanged();
    } catch (e: any) {
      setError(e?.response?.data?.error || 'Could not save');
    }
  };

  const addInspection = () =>
    submit(() =>
      procurementService.saveInspection(packageId, {
        inspectionType: draft.inspectionType || 'PDI',
        inspectionDate: draft.inspectionDate,
        location: draft.location,
        result: draft.result,
        satApplicable: !!draft.satApplicable,
      }),
    );

  const addDelivery = () =>
    submit(() =>
      procurementService.saveDelivery(packageId, {
        deliveryReferenceNumber: draft.deliveryReferenceNumber,
        deliveryDate: draft.deliveryDate,
        deliveredQuantity: draft.deliveredQuantity ? Number(draft.deliveredQuantity) : undefined,
        isFinal: !!draft.isFinal,
      }),
    );

  const addInvoice = () =>
    submit(() =>
      procurementService.saveInvoice(
        packageId,
        {
          invoiceNumber: draft.invoiceNumber,
          invoiceDate: draft.invoiceDate,
          invoiceAmount: draft.invoiceAmount ? Number(draft.invoiceAmount) : undefined,
          currency: draft.currency || 'BDT',
          supplierName: draft.supplierName,
        },
        selectedIds,
      ),
    );

  const addPayment = () =>
    submit(() =>
      procurementService.savePayment(
        packageId,
        {
          voucherNumber: draft.voucherNumber,
          paymentDate: draft.paymentDate,
          paymentAmount: draft.paymentAmount ? Number(draft.paymentAmount) : undefined,
          currency: draft.currency || 'BDT',
          bankAdviceRef: draft.bankAdviceRef,
        },
        selectedIds,
      ),
    );

  return (
    <Paper variant="outlined" sx={{ p: 2, mt: 2 }}>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {stageCode === 11 && (
        <>
          <Typography variant="subtitle2" sx={{ mb: 1 }}>Inspections</Typography>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Type</TableCell>
                <TableCell>Date</TableCell>
                <TableCell>Location</TableCell>
                <TableCell>Result</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(detail.inspections || []).map((i) => (
                <TableRow key={i.id}>
                  <TableCell>{i.inspectionType}</TableCell>
                  <TableCell>{i.inspectionDate}</TableCell>
                  <TableCell>{i.location}</TableCell>
                  <TableCell>{i.result}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <Stack direction="row" spacing={1} sx={{ mt: 2 }} alignItems="center">
            <Select
              size="small"
              value={draft.inspectionType || 'PDI'}
              onChange={(e) => set('inspectionType', e.target.value)}
            >
              <MenuItem value="PDI">PDI</MenuItem>
              <MenuItem value="PLI">PLI</MenuItem>
            </Select>
            <TextField size="small" type="date" InputLabelProps={{ shrink: true }} label="Date"
              value={draft.inspectionDate || ''} onChange={(e) => set('inspectionDate', e.target.value)} />
            <TextField size="small" label="Location" value={draft.location || ''}
              onChange={(e) => set('location', e.target.value)} />
            <TextField size="small" label="Result" value={draft.result || ''}
              onChange={(e) => set('result', e.target.value)} />
            <Button variant="contained" size="small" onClick={addInspection}>Add</Button>
          </Stack>
        </>
      )}

      {stageCode === 12 && (
        <>
          <Typography variant="subtitle2" sx={{ mb: 1 }}>
            Deliveries — cumulative {detail.cumulativeDelivered ?? 0}
          </Typography>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Reference</TableCell>
                <TableCell>Date</TableCell>
                <TableCell align="right">Quantity</TableCell>
                <TableCell align="center">Final</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(detail.deliveries || []).map((d) => (
                <TableRow key={d.id}>
                  <TableCell>{d.deliveryReferenceNumber}</TableCell>
                  <TableCell>{d.deliveryDate}</TableCell>
                  <TableCell align="right">{d.deliveredQuantity}</TableCell>
                  <TableCell align="center">{d.isFinal ? 'Yes' : ''}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <Stack direction="row" spacing={1} sx={{ mt: 2 }} alignItems="center">
            <TextField size="small" label="Reference no." value={draft.deliveryReferenceNumber || ''}
              onChange={(e) => set('deliveryReferenceNumber', e.target.value)} />
            <TextField size="small" type="date" InputLabelProps={{ shrink: true }} label="Date"
              value={draft.deliveryDate || ''} onChange={(e) => set('deliveryDate', e.target.value)} />
            <TextField size="small" type="number" label="Quantity" value={draft.deliveredQuantity || ''}
              onChange={(e) => set('deliveredQuantity', e.target.value)} />
            <Box>
              <Checkbox checked={!!draft.isFinal} onChange={(e) => set('isFinal', e.target.checked)} />
              <Typography variant="caption">Final delivery</Typography>
            </Box>
            <Button variant="contained" size="small" onClick={addDelivery}>Add</Button>
          </Stack>
        </>
      )}

      {stageCode === 13 && (
        <>
          <Typography variant="subtitle2" sx={{ mb: 1 }}>Invoices</Typography>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Invoice no.</TableCell>
                <TableCell>Date</TableCell>
                <TableCell align="right">Amount</TableCell>
                <TableCell>Supplier</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(detail.invoices || []).map((i) => (
                <TableRow key={i.id}>
                  <TableCell>{i.invoiceNumber}</TableCell>
                  <TableCell>{i.invoiceDate}</TableCell>
                  <TableCell align="right">{i.invoiceAmount}</TableCell>
                  <TableCell>{i.supplierName}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <Stack direction="row" spacing={1} sx={{ mt: 2 }} alignItems="center" flexWrap="wrap">
            <TextField size="small" label="Invoice no." value={draft.invoiceNumber || ''}
              onChange={(e) => set('invoiceNumber', e.target.value)} />
            <TextField size="small" type="date" InputLabelProps={{ shrink: true }} label="Date"
              value={draft.invoiceDate || ''} onChange={(e) => set('invoiceDate', e.target.value)} />
            <TextField size="small" type="number" label="Amount" value={draft.invoiceAmount || ''}
              onChange={(e) => set('invoiceAmount', e.target.value)} />
            <TextField size="small" label="Supplier" value={draft.supplierName || ''}
              onChange={(e) => set('supplierName', e.target.value)} />
            <FormControl size="small" sx={{ minWidth: 200 }}>
              <InputLabel>Bills deliveries</InputLabel>
              <Select
                multiple
                value={selectedIds}
                onChange={(e) => setSelectedIds(e.target.value as number[])}
                input={<OutlinedInput label="Bills deliveries" />}
                renderValue={(selected) => `${(selected as number[]).length} selected`}
              >
                {(detail.deliveries || []).map((d) => (
                  <MenuItem key={d.id} value={d.id}>
                    <Checkbox checked={selectedIds.indexOf(d.id!) > -1} />
                    <ListItemText primary={`${d.deliveryReferenceNumber} (${d.deliveryDate})`} />
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <Button variant="contained" size="small" onClick={addInvoice}>Add</Button>
          </Stack>
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
            Saving an invoice posts budget consumption automatically.
          </Typography>
        </>
      )}

      {stageCode === 14 && (
        <>
          <Typography variant="subtitle2" sx={{ mb: 1 }}>Payments</Typography>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Voucher no.</TableCell>
                <TableCell>Date</TableCell>
                <TableCell align="right">Amount</TableCell>
                <TableCell>Bank advice</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(detail.payments || []).map((p) => (
                <TableRow key={p.id}>
                  <TableCell>{p.voucherNumber}</TableCell>
                  <TableCell>{p.paymentDate}</TableCell>
                  <TableCell align="right">{p.paymentAmount}</TableCell>
                  <TableCell>{p.bankAdviceRef}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <Stack direction="row" spacing={1} sx={{ mt: 2 }} alignItems="center" flexWrap="wrap">
            <TextField size="small" label="Voucher no." value={draft.voucherNumber || ''}
              onChange={(e) => set('voucherNumber', e.target.value)} />
            <TextField size="small" type="date" InputLabelProps={{ shrink: true }} label="Date"
              value={draft.paymentDate || ''} onChange={(e) => set('paymentDate', e.target.value)} />
            <TextField size="small" type="number" label="Amount" value={draft.paymentAmount || ''}
              onChange={(e) => set('paymentAmount', e.target.value)} />
            <FormControl size="small" sx={{ minWidth: 200 }}>
              <InputLabel>Settles invoices</InputLabel>
              <Select
                multiple
                value={selectedIds}
                onChange={(e) => setSelectedIds(e.target.value as number[])}
                input={<OutlinedInput label="Settles invoices" />}
                renderValue={(selected) => `${(selected as number[]).length} selected`}
              >
                {(detail.invoices || []).map((i) => (
                  <MenuItem key={i.id} value={i.id}>
                    <Checkbox checked={selectedIds.indexOf(i.id!) > -1} />
                    <ListItemText primary={`${i.invoiceNumber} (${i.invoiceAmount})`} />
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <Button variant="contained" size="small" onClick={addPayment}>Add</Button>
          </Stack>
        </>
      )}
    </Paper>
  );
};

export default StageRecords;
