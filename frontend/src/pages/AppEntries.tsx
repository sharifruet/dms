import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Alert,
  CircularProgress,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  IconButton,
  Chip,
  Link,
  Checkbox,
  FormControlLabel,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Download as DownloadIcon,
  AttachFile as AttachFileIcon,
} from '@mui/icons-material';
import { appEntryService, AppEntry, CreateAppEntryRequest } from '../services/appEntryService';

const ALLOCATION_TYPES = ['Annual', 'Revised', 'Additional', 'Emergency'] as const;
type AllocationType = typeof ALLOCATION_TYPES[number];

// Generate fiscal year options (e.g., 2024-25, 2025-26)
const generateFiscalYears = (): string[] => {
  const years: string[] = [];
  const currentYear = new Date().getFullYear();
  for (let i = currentYear - 2; i <= currentYear + 5; i++) {
    const nextYear = (i + 1) % 100;
    years.push(`${i}-${nextYear.toString().padStart(2, '0')}`);
  }
  return years;
};

const FISCAL_YEARS = generateFiscalYears();

const AppEntries: React.FC = () => {
  const [entries, setEntries] = useState<AppEntry[]>([]);
  const [fiscalYears, setFiscalYears] = useState<number[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingEntry, setEditingEntry] = useState<AppEntry | null>(null);
  const [duplicateWarning, setDuplicateWarning] = useState<{ show: boolean; confirmed: boolean }>({
    show: false,
    confirmed: false,
  });
  
  const [formData, setFormData] = useState<CreateAppEntryRequest>({
    fiscalYear: parseInt(FISCAL_YEARS[2]?.split('-')[0] || new Date().getFullYear().toString()) || new Date().getFullYear(),
    allocationType: 'Annual',
    budgetReleaseDate: new Date().toISOString().split('T')[0],
    allocationAmount: 0,
    releaseInstallmentNo: 1,
    referenceMemoNumber: '',
    department: '',
  });
  const [attachmentFile, setAttachmentFile] = useState<File | null>(null);
  const [autoIncrementInstallment, setAutoIncrementInstallment] = useState(true);
  const [loadingNextInstallment, setLoadingNextInstallment] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  useEffect(() => {
    if (formData.fiscalYear && autoIncrementInstallment && openDialog) {
      loadNextInstallmentNo();
    }
  }, [formData.fiscalYear, autoIncrementInstallment, openDialog]);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [entriesData, fiscalYearsData] = await Promise.all([
        appEntryService.getAppEntries(),
        appEntryService.getFiscalYears(),
      ]);
      setEntries(entriesData);
      setFiscalYears(fiscalYearsData);
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to load APP entries');
    } finally {
      setLoading(false);
    }
  };

  const loadNextInstallmentNo = async () => {
    if (!formData.fiscalYear) return;
    try {
      setLoadingNextInstallment(true);
      const nextNo = await appEntryService.getNextInstallmentNo(formData.fiscalYear);
      setFormData((prev) => ({ ...prev, releaseInstallmentNo: nextNo }));
    } catch (err: any) {
      console.error('Failed to load next installment number', err);
    } finally {
      setLoadingNextInstallment(false);
    }
  };

  const handleOpenDialog = (entry?: AppEntry) => {
    if (entry) {
      setEditingEntry(entry);
      setFormData({
        fiscalYear: entry.fiscalYear,
        allocationType: entry.allocationType || 'Annual',
        budgetReleaseDate: entry.budgetReleaseDate || new Date().toISOString().split('T')[0],
        allocationAmount: entry.allocationAmount || 0,
        releaseInstallmentNo: entry.releaseInstallmentNo || 1,
        referenceMemoNumber: entry.referenceMemoNumber || '',
        department: entry.department || '',
      });
      setAutoIncrementInstallment(false);
    } else {
      setEditingEntry(null);
      const currentYear = new Date().getFullYear();
      setFormData({
        fiscalYear: currentYear,
        allocationType: 'Annual',
        budgetReleaseDate: new Date().toISOString().split('T')[0],
        allocationAmount: 0,
        releaseInstallmentNo: 1,
        referenceMemoNumber: '',
        department: '',
      });
      setAutoIncrementInstallment(true);
      loadNextInstallmentNo();
    }
    setAttachmentFile(null);
    setDuplicateWarning({ show: false, confirmed: false });
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingEntry(null);
    setAttachmentFile(null);
    setDuplicateWarning({ show: false, confirmed: false });
    setError(null);
  };

  const handleSubmit = async () => {
    // Validate required fields
    if (!formData.fiscalYear || !formData.allocationType || !formData.budgetReleaseDate || 
        !formData.allocationAmount || !formData.releaseInstallmentNo) {
      setError('Please fill in all required fields');
      return;
    }

    if (formData.allocationAmount <= 0) {
      setError('Allocation Amount must be greater than zero');
      return;
    }

    // Check for duplicate if creating new entry or if fiscal year/installment changed
    if (!editingEntry || 
        editingEntry.fiscalYear !== formData.fiscalYear || 
        editingEntry.releaseInstallmentNo !== formData.releaseInstallmentNo) {
      try {
        const isDuplicate = await appEntryService.checkDuplicate(
          formData.fiscalYear,
          formData.releaseInstallmentNo
        );

        if (isDuplicate && !duplicateWarning.confirmed) {
          setDuplicateWarning({ show: true, confirmed: false });
          return;
        }
      } catch (err: any) {
        setError('Failed to check for duplicates: ' + (err.response?.data?.error || err.message));
        return;
      }
    }

    try {
      setSubmitting(true);
      setError(null);

      const request: CreateAppEntryRequest = {
        ...formData,
        attachment: attachmentFile || undefined,
      };

      if (editingEntry) {
        await appEntryService.updateAppEntry(editingEntry.id, request);
      } else {
        await appEntryService.createAppEntry(request);
      }

      handleCloseDialog();
      await loadData();
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to save yearly budget');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this yearly budget?')) {
      return;
    }

    try {
      await appEntryService.deleteAppEntry(id);
      await loadData();
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to delete yearly budget');
    }
  };

  const formatCurrency = (amount?: number): string => {
    if (!amount) return 'N/A';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'BDT',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const formatDate = (dateString?: string): string => {
    if (!dateString) return '-';
    try {
      return new Date(dateString).toLocaleDateString();
    } catch {
      return dateString;
    }
  };

  const formatFiscalYear = (year: number): string => {
    const nextYear = (year + 1) % 100;
    return `${year}-${nextYear.toString().padStart(2, '0')}`;
  };

  const calculateTotal = (): number => {
    return entries.reduce((sum, entry) => sum + (entry.allocationAmount || 0), 0);
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" component="h1">
          Yearly Budgets
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
        >
          New Yearly Budget
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card>
        <CardContent>
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Fiscal Year</TableCell>
                  <TableCell>Allocation Type</TableCell>
                  <TableCell>Budget Release Date</TableCell>
                  <TableCell>Installment No.</TableCell>
                  <TableCell>Allocation Amount</TableCell>
                  <TableCell>Reference/Memo</TableCell>
                  <TableCell>Department</TableCell>
                  <TableCell>Attachment</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {entries.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={9} align="center">
                      <Alert severity="info">No yearly budgets found. Create your first entry above.</Alert>
                    </TableCell>
                  </TableRow>
                ) : (
                  <>
                    {entries.map((entry) => (
                      <TableRow key={entry.id} hover>
                        <TableCell>{formatFiscalYear(entry.fiscalYear)}</TableCell>
                        <TableCell>
                          <Chip label={entry.allocationType || 'N/A'} size="small" />
                        </TableCell>
                        <TableCell>{formatDate(entry.budgetReleaseDate)}</TableCell>
                        <TableCell>{entry.releaseInstallmentNo || '-'}</TableCell>
                        <TableCell>{formatCurrency(entry.allocationAmount)}</TableCell>
                        <TableCell>{entry.referenceMemoNumber || '-'}</TableCell>
                        <TableCell>{entry.department || '-'}</TableCell>
                        <TableCell>
                          {entry.attachmentFilePath ? (
                            <Link
                              href={`/api/files/${entry.attachmentFilePath}`}
                              target="_blank"
                              rel="noopener"
                              sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}
                            >
                              <AttachFileIcon fontSize="small" />
                              <DownloadIcon fontSize="small" />
                            </Link>
                          ) : (
                            '-'
                          )}
                        </TableCell>
                        <TableCell align="right">
                          <IconButton
                            size="small"
                            onClick={() => handleOpenDialog(entry)}
                            title="Edit"
                          >
                            <EditIcon fontSize="small" />
                          </IconButton>
                          <IconButton
                            size="small"
                            onClick={() => handleDelete(entry.id)}
                            color="error"
                            title="Delete"
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </TableCell>
                      </TableRow>
                    ))}
                    <TableRow sx={{ backgroundColor: 'action.selected', fontWeight: 'bold' }}>
                      <TableCell colSpan={4}>
                        <Typography variant="subtitle2" fontWeight="bold">
                          Total
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="subtitle2" fontWeight="bold">
                          {formatCurrency(calculateTotal())}
                        </Typography>
                      </TableCell>
                      <TableCell colSpan={4} />
                    </TableRow>
                  </>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      {/* Create/Edit Dialog */}
      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="md" fullWidth>
        <DialogTitle>{editingEntry ? 'Edit Yearly Budget' : 'Create New Yearly Budget'}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth required>
                <InputLabel>Fiscal Year</InputLabel>
                <Select
                  value={formData.fiscalYear}
                  label="Fiscal Year"
                  onChange={(e) => {
                    const year = parseInt(e.target.value as string);
                    setFormData({ ...formData, fiscalYear: year });
                    if (autoIncrementInstallment) {
                      loadNextInstallmentNo();
                    }
                  }}
                >
                  {FISCAL_YEARS.map((fy) => {
                    const year = parseInt(fy.split('-')[0]);
                    return (
                      <MenuItem key={fy} value={year}>
                        {fy}
                      </MenuItem>
                    );
                  })}
                </Select>
              </FormControl>
            </Grid>

            <Grid item xs={12} sm={6}>
              <FormControl fullWidth required>
                <InputLabel>Allocation / Release Type</InputLabel>
                <Select
                  value={formData.allocationType}
                  label="Allocation / Release Type"
                  onChange={(e) =>
                    setFormData({ ...formData, allocationType: e.target.value as AllocationType })
                  }
                >
                  {ALLOCATION_TYPES.map((type) => (
                    <MenuItem key={type} value={type}>
                      {type}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                required
                label="Budget Release Date"
                type="date"
                value={formData.budgetReleaseDate}
                onChange={(e) => setFormData({ ...formData, budgetReleaseDate: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                required
                label="Allocation Amount (BDT)"
                type="number"
                value={formData.allocationAmount || ''}
                onChange={(e) =>
                  setFormData({ ...formData, allocationAmount: parseFloat(e.target.value) || 0 })
                }
                inputProps={{ min: 0, step: 0.01 }}
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <Box display="flex" alignItems="center" gap={1}>
                <TextField
                  fullWidth
                  required
                  label="Release Installment No."
                  type="number"
                  value={formData.releaseInstallmentNo || ''}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      releaseInstallmentNo: parseInt(e.target.value) || 1,
                    })
                  }
                  inputProps={{ min: 1 }}
                  disabled={autoIncrementInstallment}
                  InputProps={{
                    endAdornment: loadingNextInstallment ? (
                      <CircularProgress size={20} />
                    ) : undefined,
                  }}
                />
                {!editingEntry && (
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={autoIncrementInstallment}
                        onChange={(e) => {
                          setAutoIncrementInstallment(e.target.checked);
                          if (e.target.checked) {
                            loadNextInstallmentNo();
                          }
                        }}
                      />
                    }
                    label="Auto"
                    sx={{ minWidth: 80 }}
                  />
                )}
              </Box>
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Reference / Memo Number"
                value={formData.referenceMemoNumber || ''}
                onChange={(e) =>
                  setFormData({ ...formData, referenceMemoNumber: e.target.value })
                }
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Department"
                value={formData.department || ''}
                onChange={(e) => setFormData({ ...formData, department: e.target.value })}
              />
            </Grid>

            <Grid item xs={12}>
              <Button
                variant="outlined"
                component="label"
                startIcon={<AttachFileIcon />}
                fullWidth
              >
                {attachmentFile ? attachmentFile.name : 'Upload Attachment'}
                <input
                  type="file"
                  hidden
                  onChange={(e) => {
                    const file = e.target.files?.[0];
                    if (file) {
                      setAttachmentFile(file);
                    }
                  }}
                />
              </Button>
              {attachmentFile && (
                <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                  Selected: {attachmentFile.name} ({(attachmentFile.size / 1024 / 1024).toFixed(2)} MB)
                </Typography>
              )}
            </Grid>

            {duplicateWarning.show && (
              <Grid item xs={12}>
                <Alert severity="warning">
                  A yearly budget already exists for Fiscal Year {formatFiscalYear(formData.fiscalYear)} and Installment No{' '}
                  {formData.releaseInstallmentNo}. Do you want to proceed anyway?
                </Alert>
              </Grid>
            )}
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog} disabled={submitting}>
            Cancel
          </Button>
          {duplicateWarning.show && !duplicateWarning.confirmed && (
            <Button
              onClick={() => setDuplicateWarning({ show: true, confirmed: true })}
              color="warning"
            >
              Proceed Anyway
            </Button>
          )}
          <Button
            onClick={handleSubmit}
            variant="contained"
            disabled={submitting || (duplicateWarning.show && !duplicateWarning.confirmed)}
          >
            {submitting ? <CircularProgress size={24} /> : editingEntry ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default AppEntries;
