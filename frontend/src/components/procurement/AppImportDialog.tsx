import React, { useState } from 'react';
import {
  Alert,
  AlertTitle,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import procurementService from '../../services/procurementService';
import { AppImportOutcome, AppImportReport } from '../../types/procurement';

interface Props {
  open: boolean;
  onClose: () => void;
  onImported: () => void;
}

/**
 * Stage 1: create packages in bulk from an APP workbook (REQ-1.1).
 *
 * The dialog leads with a dry run. An APP is the front door of the whole lifecycle, and
 * a workbook with a surprise in it — a repeated package, a row with no cost — is much
 * cheaper to look at before it lands than to unpick afterwards.
 */
const AppImportDialog: React.FC<Props> = ({ open, onClose, onImported }) => {
  const [file, setFile] = useState<File | null>(null);
  const [department, setDepartment] = useState('BPDB');
  const [report, setReport] = useState<AppImportReport | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reset = () => {
    setFile(null);
    setReport(null);
    setError(null);
    setBusy(false);
  };

  const handleClose = () => {
    reset();
    onClose();
  };

  const run = async (dryRun: boolean) => {
    if (!file) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const result = await procurementService.importApp(file, { department, dryRun });
      setReport(result);
      if (!dryRun) {
        onImported();
      }
    } catch (e: any) {
      setError(e?.response?.data?.error || 'Could not read that workbook');
    } finally {
      setBusy(false);
    }
  };

  const outcomeTable = (title: string, rows: AppImportOutcome[], severity?: 'error' | 'warning') => {
    if (rows.length === 0) {
      return null;
    }
    return (
      <>
        <Typography
          variant="subtitle2"
          sx={{ mt: 2, mb: 1 }}
          color={severity === 'error' ? 'error' : undefined}
        >
          {title} ({rows.length})
        </Typography>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Package</TableCell>
              <TableCell>Where</TableCell>
              <TableCell>Reason</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((row, i) => (
              <TableRow key={i}>
                <TableCell>{row.packageNumber || '—'}</TableCell>
                <TableCell>{row.origin}</TableCell>
                <TableCell>{row.reason}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </>
    );
  };

  return (
    <Dialog open={open} onClose={handleClose} fullWidth maxWidth="md">
      <DialogTitle>Import an APP workbook</DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Each package row becomes a procurement package opened at Stage 1. Packages that
          already exist are left exactly as they are, so re-uploading a corrected workbook
          will not undo work already done.
        </Typography>

        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

        <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 2 }}>
          <Button component="label" variant="outlined" startIcon={<UploadFileIcon />}>
            {file ? 'Choose a different file' : 'Choose workbook'}
            <input
              hidden
              type="file"
              accept=".xls,.xlsx"
              onChange={(e) => {
                setFile(e.target.files?.[0] ?? null);
                setReport(null);
              }}
            />
          </Button>
          {file && <Chip label={file.name} onDelete={() => { setFile(null); setReport(null); }} />}
          <TextField
            size="small"
            label="Department"
            value={department}
            onChange={(e) => setDepartment(e.target.value)}
          />
        </Stack>

        {report && (
          <>
            <Alert severity={report.failedCount > 0 ? 'warning' : 'success'}>
              <AlertTitle>
                {report.dryRun ? 'Dry run — nothing has been saved' : 'Import complete'}
              </AlertTitle>
              Read {report.rowsRead} package {report.rowsRead === 1 ? 'row' : 'rows'}
              {report.fiscalYear ? ` for FY ${report.fiscalYear}` : ''}:{' '}
              <strong>{report.createdCount}</strong>{' '}
              {report.dryRun ? 'would be created' : 'created'}
              {report.skippedCount > 0 && `, ${report.skippedCount} skipped`}
              {report.failedCount > 0 && `, ${report.failedCount} could not be read`}.
              {report.skippedSheets.length > 0 && (
                <div>
                  Sheets without an APP table were ignored:{' '}
                  {report.skippedSheets.join(', ')}.
                </div>
              )}
            </Alert>

            {outcomeTable('Could not be imported', report.failed, 'error')}
            {outcomeTable('Worth checking', report.warnings, 'warning')}
            {outcomeTable('Skipped', report.skipped)}
            {outcomeTable(report.dryRun ? 'Would be created' : 'Created', report.created)}
          </>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose}>Close</Button>
        <Button disabled={!file || busy} onClick={() => run(true)}>
          Preview without saving
        </Button>
        <Button
          variant="contained"
          disabled={!file || busy || (report != null && !report.dryRun)}
          onClick={() => run(false)}
        >
          Import
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default AppImportDialog;
