import React, { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Grid,
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
import {
  BudgetSummary,
  DepartmentBudget,
  DepartmentBudgetPosition,
} from '../../types/procurement';
import useProcurementRole from '../../hooks/useProcurementRole';

interface Props {
  packageId: number;
}

const money = (value?: number) =>
  value === undefined || value === null
    ? '—'
    : Number(value).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/**
 * Budget position for a package.
 *
 * Allocation, release, revision and additional are entered here. Consumption is not
 * editable - it is posted from verified invoices, and Remaining is derived from both.
 */
const BudgetPanel: React.FC<Props> = ({ packageId }) => {
  const [summary, setSummary] = useState<BudgetSummary | null>(null);
  const [entries, setEntries] = useState<any[]>([]);
  const [consumption, setConsumption] = useState<any[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [draft, setDraft] = useState<Record<string, any>>({ entryType: 'ALLOCATION', currency: 'BDT' });
  const [department, setDepartment] = useState<DepartmentBudget | null>(null);
  const [position, setPosition] = useState<DepartmentBudgetPosition | null>(null);
  const { canApprove } = useProcurementRole();

  const load = useCallback(async () => {
    try {
      const data = await procurementService.getBudget(packageId);
      setSummary(data.summary);
      setEntries(data.entries || []);
      setConsumption(data.consumption || []);
      // Present only once the department's annual budget for this fiscal year exists
      setDepartment(data.departmentBudget || null);
      setPosition(data.departmentPosition || null);
    } catch (e: any) {
      setError(e?.response?.data?.error || 'Could not load the budget');
    }
  }, [packageId]);

  useEffect(() => {
    load();
  }, [load]);

  const add = async () => {
    setError(null);
    try {
      await procurementService.addBudgetEntry(packageId, {
        entryType: draft.entryType,
        amount: Number(draft.amount),
        currency: draft.currency,
        effectiveDate: draft.effectiveDate,
        reason: draft.reason,
      });
      setDraft({ entryType: 'ALLOCATION', currency: 'BDT' });
      await load();
    } catch (e: any) {
      setError(e?.response?.data?.error || 'Could not save the entry');
    }
  };

  const tile = (label: string, value?: number, color?: string) => (
    <Grid item xs={6} md={3}>
      <Card variant="outlined">
        <CardContent>
          <Typography variant="caption" color="text.secondary">{label}</Typography>
          <Typography variant="h6" sx={{ color: color || 'text.primary' }}>{money(value)}</Typography>
        </CardContent>
      </Card>
    </Grid>
  );

  return (
    <Box sx={{ p: 3, overflow: 'auto' }}>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {summary?.overspent && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Consumption has exceeded the available budget for this package.
        </Alert>
      )}
      {summary?.lowBudget && !summary?.overspent && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          Less than 10% of the available budget remains.
        </Alert>
      )}

      {/*
        Q-13: budget is allocated annually at department level and drawn down per package.
        Showing only the package figure hides whether there is anything left to draw from.
      */}
      {position && (
        <Paper variant="outlined" sx={{ p: 2, mb: 3 }}>
          <Typography variant="subtitle2" sx={{ mb: 1 }}>
            {department?.department ?? position.department} — annual budget
            {position.fiscalYear ? ` for FY ${position.fiscalYear}` : ''}
          </Typography>
          {position.overCommitted && (
            <Alert severity="warning" sx={{ mb: 2 }}>
              Packages in this department have been allocated more than the annual budget
              holds. This is a planning control rather than a payment gate — nothing is
              blocked — but the figures no longer add up.
            </Alert>
          )}
          <Grid container spacing={2}>
            {tile('Department allocated', position.allocated)}
            {tile('Committed to packages', position.committed)}
            {tile(
              'Left to draw down',
              position.remaining,
              position.overCommitted ? '#d32f2f' : '#2e7d32',
            )}
          </Grid>
        </Paper>
      )}

      {!position && (
        <Alert severity="info" sx={{ mb: 3 }}>
          No annual budget is on file for this department and fiscal year, so there is
          nothing for this package to draw down from.
          {canApprove
            ? ' Set one to track the departmental position.'
            : ' A Checker can set one.'}
        </Alert>
      )}

      <Typography variant="subtitle2" sx={{ mb: 1 }}>
        This package
      </Typography>
      <Grid container spacing={2} sx={{ mb: 3 }}>
        {tile('Allocation', summary?.totalAllocation)}
        {tile('Released', summary?.totalRelease)}
        {tile('Consumed (from invoices)', summary?.totalConsumption)}
        {tile(
          'Remaining',
          summary?.remaining,
          summary?.overspent ? '#d32f2f' : summary?.lowBudget ? '#ed6c02' : '#2e7d32',
        )}
      </Grid>

      {/* Budget entry and approval are Checker actions (Q-13, Q-17) */}
      <Paper variant="outlined" sx={{ p: 2, mb: 3, display: canApprove ? 'block' : 'none' }}>
        <Typography variant="subtitle2" sx={{ mb: 1 }}>Add a budget entry</Typography>
        <Stack direction="row" spacing={1} flexWrap="wrap" alignItems="center">
          <TextField
            select size="small" label="Type" sx={{ minWidth: 160 }}
            value={draft.entryType}
            onChange={(e) => setDraft({ ...draft, entryType: e.target.value })}
          >
            <MenuItem value="ALLOCATION">Allocation</MenuItem>
            <MenuItem value="RELEASE">Release</MenuItem>
            <MenuItem value="REVISION">Revision</MenuItem>
            <MenuItem value="ADDITIONAL">Additional</MenuItem>
          </TextField>
          <TextField size="small" type="number" label="Amount" value={draft.amount || ''}
            onChange={(e) => setDraft({ ...draft, amount: e.target.value })} />
          <TextField size="small" label="Currency" sx={{ width: 100 }} value={draft.currency || 'BDT'}
            onChange={(e) => setDraft({ ...draft, currency: e.target.value })} />
          <TextField size="small" type="date" label="Effective" InputLabelProps={{ shrink: true }}
            value={draft.effectiveDate || ''} onChange={(e) => setDraft({ ...draft, effectiveDate: e.target.value })} />
          <TextField size="small" label="Reason" sx={{ minWidth: 220 }} value={draft.reason || ''}
            onChange={(e) => setDraft({ ...draft, reason: e.target.value })}
            helperText={draft.entryType === 'REVISION' ? 'Required for a revision' : ' '} />
          <Button variant="contained" onClick={add} disabled={!draft.amount}>Add</Button>
        </Stack>
      </Paper>

      <Paper variant="outlined" sx={{ p: 2, mb: 3 }}>
        <Typography variant="subtitle2" sx={{ mb: 1 }}>Entries</Typography>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Type</TableCell>
              <TableCell align="right">Amount</TableCell>
              <TableCell>Effective</TableCell>
              <TableCell>Reason</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {entries.map((e) => (
              <TableRow key={e.id}>
                <TableCell>{e.entryType}</TableCell>
                <TableCell align="right">{money(e.amount)}</TableCell>
                <TableCell>{e.effectiveDate}</TableCell>
                <TableCell>{e.reason}</TableCell>
              </TableRow>
            ))}
            {entries.length === 0 && (
              <TableRow>
                <TableCell colSpan={4}>
                  <Typography variant="body2" color="text.secondary">No budget entries yet.</Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>

      <Paper variant="outlined" sx={{ p: 2 }}>
        <Typography variant="subtitle2" sx={{ mb: 1 }}>
          Consumption (posted automatically from invoices)
        </Typography>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Invoice</TableCell>
              <TableCell align="right">Consumed</TableCell>
              <TableCell>Posted</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {consumption.map((c) => (
              <TableRow key={c.id}>
                <TableCell>#{c.invoiceId}</TableCell>
                <TableCell align="right">{money(c.consumedAmount)}</TableCell>
                <TableCell>{c.postedAt?.substring(0, 10)}</TableCell>
              </TableRow>
            ))}
            {consumption.length === 0 && (
              <TableRow>
                <TableCell colSpan={3}>
                  <Typography variant="body2" color="text.secondary">
                    Nothing consumed yet — consumption appears when invoices are recorded at stage 13.
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>
    </Box>
  );
};

export default BudgetPanel;
