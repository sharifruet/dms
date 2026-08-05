import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
  TablePagination,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import procurementService from '../../services/procurementService';
import { ProcurementPackage, STAGE_COUNT, STAGE_NAMES } from '../../types/procurement';

/**
 * All packages, with where each one has got to. This replaces the document list as the
 * entry point of the system.
 */
const PackageList: React.FC = () => {
  const navigate = useNavigate();
  const [packages, setPackages] = useState<ProcurementPackage[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(25);
  const [query, setQuery] = useState('');
  const [stage, setStage] = useState<string>('');
  const [status, setStatus] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [draft, setDraft] = useState<Record<string, string>>({});

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await procurementService.listPackages({
        query: query || undefined,
        stage: stage ? Number(stage) : undefined,
        status: status || undefined,
        page,
        size,
      });
      setPackages(data.content || []);
      setTotal(data.totalElements || 0);
    } catch (e: any) {
      setError(e?.response?.data?.error || 'Could not load packages');
    } finally {
      setLoading(false);
    }
  }, [query, stage, status, page, size]);

  useEffect(() => {
    const timer = setTimeout(load, 250);
    return () => clearTimeout(timer);
  }, [load]);

  const create = async () => {
    setError(null);
    try {
      const created = await procurementService.createPackage({
        packageNumber: draft.packageNumber,
        packageDescription: draft.packageDescription,
        department: draft.department,
        approvingAuthority: draft.approvingAuthority,
        priceLacBdt: draft.priceLacBdt ? Number(draft.priceLacBdt) : undefined,
        lotNumber: draft.lotNumber || undefined,
      });
      setCreateOpen(false);
      setDraft({});
      navigate(`/procurement/packages/${created.id}`);
    } catch (e: any) {
      setError(e?.response?.data?.error || 'Could not create the package');
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Stack direction="row" alignItems="center" spacing={2} sx={{ mb: 2 }}>
        <Typography variant="h5" sx={{ flexGrow: 1 }}>Procurement packages</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
          New package
        </Button>
      </Stack>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}

      <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
        <Stack direction="row" spacing={2} flexWrap="wrap">
          <TextField
            size="small" label="Search package number or description" sx={{ minWidth: 320 }}
            value={query} onChange={(e) => { setQuery(e.target.value); setPage(0); }}
          />
          <TextField
            select size="small" label="Stage" sx={{ minWidth: 200 }}
            value={stage} onChange={(e) => { setStage(e.target.value); setPage(0); }}
          >
            <MenuItem value="">All stages</MenuItem>
            {Array.from({ length: STAGE_COUNT }, (_, i) => i + 1).map((s) => (
              <MenuItem key={s} value={String(s)}>{s}. {STAGE_NAMES[s]}</MenuItem>
            ))}
          </TextField>
          <TextField
            select size="small" label="Status" sx={{ minWidth: 160 }}
            value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}
          >
            <MenuItem value="">All</MenuItem>
            <MenuItem value="ACTIVE">Active</MenuItem>
            <MenuItem value="CLOSED">Closed</MenuItem>
          </TextField>
        </Stack>
      </Paper>

      <Paper variant="outlined">
        {loading && <LinearProgress />}
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Package</TableCell>
              <TableCell>Description</TableCell>
              <TableCell>Department</TableCell>
              <TableCell>Current stage</TableCell>
              <TableCell align="right">APP value (lac BDT)</TableCell>
              <TableCell>Status</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {packages.map((p) => (
              <TableRow
                key={p.id}
                hover
                sx={{ cursor: 'pointer' }}
                onClick={() => navigate(`/procurement/packages/${p.id}`)}
              >
                <TableCell>
                  <Typography variant="body2" sx={{ fontWeight: 500 }}>
                    {p.packageNumber}
                  </Typography>
                  {p.lotNumber && (
                    <Chip label={`Lot ${p.lotNumber}`} size="small" sx={{ height: 18, mt: 0.5 }} />
                  )}
                </TableCell>
                <TableCell sx={{ maxWidth: 320 }}>
                  <Typography variant="body2" noWrap>{p.packageDescription}</Typography>
                </TableCell>
                <TableCell>{p.department}</TableCell>
                <TableCell>
                  <Chip
                    size="small"
                    label={`${p.currentStage}. ${STAGE_NAMES[p.currentStage] || ''}`}
                    color={p.currentStage >= STAGE_COUNT ? 'success' : 'primary'}
                    variant="outlined"
                  />
                </TableCell>
                <TableCell align="right">
                  {p.priceLacBdt !== undefined && p.priceLacBdt !== null
                    ? Number(p.priceLacBdt).toLocaleString()
                    : '—'}
                </TableCell>
                <TableCell>
                  <Chip
                    size="small"
                    label={p.status}
                    color={p.status === 'CLOSED' ? 'default' : 'primary'}
                  />
                </TableCell>
              </TableRow>
            ))}
            {!loading && packages.length === 0 && (
              <TableRow>
                <TableCell colSpan={6}>
                  <Box sx={{ py: 4, textAlign: 'center' }}>
                    <Typography variant="body2" color="text.secondary">
                      No packages yet. Create one, or import an APP to create them in bulk.
                    </Typography>
                  </Box>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
        <TablePagination
          component="div"
          count={total}
          page={page}
          rowsPerPage={size}
          onPageChange={(_, p) => setPage(p)}
          onRowsPerPageChange={(e) => { setSize(Number(e.target.value)); setPage(0); }}
          rowsPerPageOptions={[10, 25, 50, 100]}
        />
      </Paper>

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>New procurement package</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              autoFocus label="Package number" required fullWidth
              helperText="The key every document and value in the lifecycle hangs off"
              value={draft.packageNumber || ''}
              onChange={(e) => setDraft({ ...draft, packageNumber: e.target.value })}
            />
            <TextField
              label="Lot number (optional)" fullWidth
              helperText="Set when an APP line is tendered in lots"
              value={draft.lotNumber || ''}
              onChange={(e) => setDraft({ ...draft, lotNumber: e.target.value })}
            />
            <TextField
              label="Description" fullWidth multiline minRows={2}
              value={draft.packageDescription || ''}
              onChange={(e) => setDraft({ ...draft, packageDescription: e.target.value })}
            />
            <TextField
              label="Department" fullWidth
              value={draft.department || ''}
              onChange={(e) => setDraft({ ...draft, department: e.target.value })}
            />
            <TextField
              label="Approving authority" fullWidth
              value={draft.approvingAuthority || ''}
              onChange={(e) => setDraft({ ...draft, approvingAuthority: e.target.value })}
            />
            <TextField
              label="APP value (lac BDT)" type="number" fullWidth
              value={draft.priceLacBdt || ''}
              onChange={(e) => setDraft({ ...draft, priceLacBdt: e.target.value })}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
          <Button variant="contained" disabled={!draft.packageNumber} onClick={create}>
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default PackageList;
