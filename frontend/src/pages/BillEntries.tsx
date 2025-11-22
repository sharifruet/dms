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
  Chip,
  Alert,
  CircularProgress,
  TextField,
  InputAdornment,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  IconButton,
} from '@mui/material';
import {
  Search as SearchIcon,
  Receipt as BillIcon,
  FilterList as FilterIcon,
  Add as AddIcon,
  Delete as DeleteIcon,
} from '@mui/icons-material';
import { financeService, BillHeader, BillLine } from '../services/financeService';

interface BillLineForm {
  projectIdentifier?: string;
  department?: string;
  costCenter?: string;
  category?: string;
  amount: number;
  taxAmount?: number;
}

const BillEntries: React.FC = () => {
  const [bills, setBills] = useState<BillHeader[]>([]);
  const [selectedBill, setSelectedBill] = useState<BillHeader | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [fiscalYearFilter, setFiscalYearFilter] = useState<number | ''>('');
  const [vendorFilter, setVendorFilter] = useState('');
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [newBill, setNewBill] = useState({
    fiscalYear: new Date().getFullYear(),
    vendor: '',
    invoiceNumber: '',
    invoiceDate: new Date().toISOString().split('T')[0],
    lines: [{ amount: 0, taxAmount: 0 }] as BillLineForm[],
  });
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    loadBills();
  }, [fiscalYearFilter, vendorFilter]);

  const loadBills = async () => {
    try {
      setLoading(true);
      setError(null);
      const billsResponse = await financeService.getBills(
        fiscalYearFilter ? Number(fiscalYearFilter) : undefined,
        vendorFilter || undefined
      );
      const billsArray = Array.isArray(billsResponse) ? billsResponse : (billsResponse.content || []);
      setBills(billsArray);
      // Load full details for selected bill if it exists
      if (selectedBill) {
        const updatedBill = billsArray.find(b => b.id === selectedBill.id);
        if (updatedBill) {
          loadBillDetails(updatedBill.id);
        }
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load bills');
    } finally {
      setLoading(false);
    }
  };

  const loadBillDetails = async (billId: number) => {
    try {
      const bill = await financeService.getBill(billId);
      setSelectedBill(bill);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load bill details');
    }
  };

  const formatCurrency = (amount: number): string => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
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

  const calculateTotal = (lines?: BillLine[]): number => {
    if (!lines) return 0;
    return lines.reduce((sum, line) => sum + (line.amount || 0), 0);
  };

  const calculateTaxTotal = (lines?: BillLine[]): number => {
    if (!lines) return 0;
    return lines.reduce((sum, line) => sum + (line.taxAmount || 0), 0);
  };

  const uniqueFiscalYears = Array.from(
    new Set(bills.map(b => b.fiscalYear).filter(y => y != null))
  ).sort((a, b) => b - a);

  const uniqueVendors = Array.from(
    new Set(bills.map(b => b.vendor).filter(v => v != null && v.trim() !== ''))
  ).sort();

  const handleCreateBill = async () => {
    if (!newBill.fiscalYear || !newBill.invoiceDate || newBill.lines.length === 0) {
      setError('Please fill in all required fields (Fiscal Year, Invoice Date, and at least one line item)');
      return;
    }

    // Validate that all lines have amounts
    if (newBill.lines.some(line => !line.amount || line.amount <= 0)) {
      setError('All line items must have a positive amount');
      return;
    }

    try {
      setCreating(true);
      setError(null);
      const billId = await financeService.createBill({
        fiscalYear: newBill.fiscalYear,
        vendor: newBill.vendor || undefined,
        invoiceNumber: newBill.invoiceNumber || undefined,
        invoiceDate: newBill.invoiceDate,
        lines: newBill.lines.map(line => ({
          projectIdentifier: line.projectIdentifier || undefined,
          department: line.department || undefined,
          costCenter: line.costCenter || undefined,
          category: line.category || undefined,
          amount: line.amount,
          taxAmount: line.taxAmount || undefined,
        })),
      });
      
      setCreateDialogOpen(false);
      setNewBill({
        fiscalYear: new Date().getFullYear(),
        vendor: '',
        invoiceNumber: '',
        invoiceDate: new Date().toISOString().split('T')[0],
        lines: [{ amount: 0, taxAmount: 0 }],
      });
      await loadBills();
      // Select the newly created bill
      await loadBillDetails(billId);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create bill');
    } finally {
      setCreating(false);
    }
  };

  const addBillLine = () => {
    setNewBill({
      ...newBill,
      lines: [...newBill.lines, { amount: 0, taxAmount: 0 }],
    });
  };

  const removeBillLine = (index: number) => {
    setNewBill({
      ...newBill,
      lines: newBill.lines.filter((_, i) => i !== index),
    });
  };

  const updateBillLine = (index: number, field: keyof BillLineForm, value: any) => {
    const updatedLines = [...newBill.lines];
    updatedLines[index] = { ...updatedLines[index], [field]: value };
    setNewBill({ ...newBill, lines: updatedLines });
  };

  const filteredBills = bills.filter((bill) =>
    (bill.invoiceNumber || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (bill.vendor || '').toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (loading && bills.length === 0) {
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
          Bill Entries
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setCreateDialogOpen(true)}
        >
          Create Bill
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Grid container spacing={3}>
        {/* Bills List */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Bills ({bills.length})
              </Typography>

              <Grid container spacing={2} sx={{ mb: 2 }}>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    size="small"
                    placeholder="Search bills..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    InputProps={{
                      startAdornment: (
                        <InputAdornment position="start">
                          <SearchIcon />
                        </InputAdornment>
                      ),
                    }}
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <FormControl fullWidth size="small">
                    <InputLabel>Fiscal Year</InputLabel>
                    <Select
                      value={fiscalYearFilter}
                      label="Fiscal Year"
                      onChange={(e) => setFiscalYearFilter(e.target.value as number | '')}
                    >
                      <MenuItem value="">All Years</MenuItem>
                      {uniqueFiscalYears.map((year) => (
                        <MenuItem key={year} value={year}>
                          {year}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>
                <Grid item xs={12} sm={6}>
                  <FormControl fullWidth size="small">
                    <InputLabel>Vendor</InputLabel>
                    <Select
                      value={vendorFilter}
                      label="Vendor"
                      onChange={(e) => setVendorFilter(e.target.value)}
                    >
                      <MenuItem value="">All Vendors</MenuItem>
                      {uniqueVendors.map((vendor) => (
                        <MenuItem key={vendor} value={vendor}>
                          {vendor}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>
                {(fiscalYearFilter || vendorFilter) && (
                  <Grid item xs={12}>
                    <Button
                      fullWidth
                      variant="outlined"
                      size="small"
                      startIcon={<FilterIcon />}
                      onClick={() => {
                        setFiscalYearFilter('');
                        setVendorFilter('');
                      }}
                    >
                      Clear Filters
                    </Button>
                  </Grid>
                )}
              </Grid>

              {filteredBills.length === 0 ? (
                <Alert severity="info">No bills found.</Alert>
              ) : (
                <Box sx={{ maxHeight: 600, overflow: 'auto' }}>
                  {filteredBills.map((bill) => (
                    <Card
                      key={bill.id}
                      sx={{
                        mb: 1,
                        cursor: 'pointer',
                        border: selectedBill?.id === bill.id ? 2 : 1,
                        borderColor: selectedBill?.id === bill.id ? 'primary.main' : 'divider',
                        '&:hover': {
                          backgroundColor: 'action.hover',
                        },
                      }}
                      onClick={() => loadBillDetails(bill.id)}
                    >
                      <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
                        <Box display="flex" alignItems="center" gap={1} mb={1}>
                          <BillIcon color="primary" />
                          <Typography variant="subtitle2" noWrap>
                            {bill.invoiceNumber || `Bill #${bill.id}`}
                          </Typography>
                        </Box>
                        <Box display="flex" flexDirection="column" gap={0.5}>
                          {bill.vendor && (
                            <Typography variant="body2" color="text.secondary">
                              {bill.vendor}
                            </Typography>
                          )}
                          <Box display="flex" gap={1} flexWrap="wrap">
                            <Chip label={`FY ${bill.fiscalYear}`} size="small" />
                            {bill.invoiceDate && (
                              <Typography variant="caption" color="text.secondary">
                                {formatDate(bill.invoiceDate)}
                              </Typography>
                            )}
                          </Box>
                        </Box>
                      </CardContent>
                    </Card>
                  ))}
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Bill Details Table */}
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                {selectedBill
                  ? `Bill Details: ${selectedBill.invoiceNumber || `Bill #${selectedBill.id}`}`
                  : 'Select a bill to view details'}
              </Typography>

              {!selectedBill ? (
                <Alert severity="info">
                  Please select a bill from the list to view its details.
                </Alert>
              ) : loading ? (
                <Box display="flex" justifyContent="center" p={3}>
                  <CircularProgress />
                </Box>
              ) : !selectedBill.lines || selectedBill.lines.length === 0 ? (
                <Alert severity="warning">
                  No line items found for this bill.
                </Alert>
              ) : (
                <>
                  <Box mb={2}>
                    <Grid container spacing={2}>
                      <Grid item xs={12} sm={6}>
                        <Typography variant="body2" color="text.secondary">
                          Vendor
                        </Typography>
                        <Typography variant="body1">
                          {selectedBill.vendor || '-'}
                        </Typography>
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <Typography variant="body2" color="text.secondary">
                          Invoice Date
                        </Typography>
                        <Typography variant="body1">
                          {formatDate(selectedBill.invoiceDate)}
                        </Typography>
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <Typography variant="body2" color="text.secondary">
                          Fiscal Year
                        </Typography>
                        <Typography variant="body1">
                          {selectedBill.fiscalYear}
                        </Typography>
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <Typography variant="body2" color="text.secondary">
                          Invoice Number
                        </Typography>
                        <Typography variant="body1">
                          {selectedBill.invoiceNumber || '-'}
                        </Typography>
                      </Grid>
                    </Grid>
                  </Box>

                  <TableContainer component={Paper} sx={{ maxHeight: 500 }}>
                    <Table stickyHeader>
                      <TableHead>
                        <TableRow>
                          <TableCell>Project</TableCell>
                          <TableCell>Department</TableCell>
                          <TableCell>Cost Center</TableCell>
                          <TableCell>Category</TableCell>
                          <TableCell align="right">Amount</TableCell>
                          <TableCell align="right">Tax</TableCell>
                          <TableCell align="right">Total</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {selectedBill.lines.map((line) => (
                          <TableRow key={line.id} hover>
                            <TableCell>{line.projectIdentifier || '-'}</TableCell>
                            <TableCell>{line.department || '-'}</TableCell>
                            <TableCell>{line.costCenter || '-'}</TableCell>
                            <TableCell>{line.category || '-'}</TableCell>
                            <TableCell align="right">
                              {formatCurrency(line.amount || 0)}
                            </TableCell>
                            <TableCell align="right">
                              {formatCurrency(line.taxAmount || 0)}
                            </TableCell>
                            <TableCell align="right">
                              {formatCurrency((line.amount || 0) + (line.taxAmount || 0))}
                            </TableCell>
                          </TableRow>
                        ))}
                        <TableRow sx={{ backgroundColor: 'action.selected', fontWeight: 'bold' }}>
                          <TableCell colSpan={4}>
                            <Typography variant="subtitle2" fontWeight="bold">
                              Total
                            </Typography>
                          </TableCell>
                          <TableCell align="right">
                            <Typography variant="subtitle2" fontWeight="bold">
                              {formatCurrency(calculateTotal(selectedBill.lines))}
                            </Typography>
                          </TableCell>
                          <TableCell align="right">
                            <Typography variant="subtitle2" fontWeight="bold">
                              {formatCurrency(calculateTaxTotal(selectedBill.lines))}
                            </Typography>
                          </TableCell>
                          <TableCell align="right">
                            <Typography variant="subtitle2" fontWeight="bold">
                              {formatCurrency(
                                calculateTotal(selectedBill.lines) + calculateTaxTotal(selectedBill.lines)
                              )}
                            </Typography>
                          </TableCell>
                        </TableRow>
                      </TableBody>
                    </Table>
                  </TableContainer>

                  <Box mt={2} display="flex" justifyContent="space-between" alignItems="center">
                    <Typography variant="body2" color="text.secondary">
                      {selectedBill.lines.length} line items
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Grand Total: {formatCurrency(
                        calculateTotal(selectedBill.lines) + calculateTaxTotal(selectedBill.lines)
                      )}
                    </Typography>
                  </Box>
                </>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Create Bill Dialog */}
      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Create New Bill</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                required
                label="Fiscal Year"
                type="number"
                value={newBill.fiscalYear}
                onChange={(e) => setNewBill({ ...newBill, fiscalYear: parseInt(e.target.value) || new Date().getFullYear() })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Invoice Date"
                type="date"
                required
                value={newBill.invoiceDate}
                onChange={(e) => setNewBill({ ...newBill, invoiceDate: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Vendor"
                value={newBill.vendor}
                onChange={(e) => setNewBill({ ...newBill, vendor: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Invoice Number"
                value={newBill.invoiceNumber}
                onChange={(e) => setNewBill({ ...newBill, invoiceNumber: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                <Typography variant="h6">Line Items</Typography>
                <Button
                  size="small"
                  startIcon={<AddIcon />}
                  onClick={addBillLine}
                >
                  Add Line
                </Button>
              </Box>
            </Grid>
            {newBill.lines.map((line, index) => (
              <Grid item xs={12} key={index}>
                <Card variant="outlined">
                  <CardContent>
                    <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                      <Typography variant="subtitle2">Line {index + 1}</Typography>
                      {newBill.lines.length > 1 && (
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => removeBillLine(index)}
                        >
                          <DeleteIcon />
                        </IconButton>
                      )}
                    </Box>
                    <Grid container spacing={2}>
                      <Grid item xs={12} sm={6}>
                        <TextField
                          fullWidth
                          size="small"
                          label="Project Identifier"
                          value={line.projectIdentifier || ''}
                          onChange={(e) => updateBillLine(index, 'projectIdentifier', e.target.value)}
                        />
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <TextField
                          fullWidth
                          size="small"
                          label="Department"
                          value={line.department || ''}
                          onChange={(e) => updateBillLine(index, 'department', e.target.value)}
                        />
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <TextField
                          fullWidth
                          size="small"
                          label="Cost Center"
                          value={line.costCenter || ''}
                          onChange={(e) => updateBillLine(index, 'costCenter', e.target.value)}
                        />
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <TextField
                          fullWidth
                          size="small"
                          label="Category"
                          value={line.category || ''}
                          onChange={(e) => updateBillLine(index, 'category', e.target.value)}
                        />
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <TextField
                          fullWidth
                          required
                          size="small"
                          label="Amount"
                          type="number"
                          value={line.amount || ''}
                          onChange={(e) => updateBillLine(index, 'amount', parseFloat(e.target.value) || 0)}
                          inputProps={{ min: 0, step: 0.01 }}
                        />
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <TextField
                          fullWidth
                          size="small"
                          label="Tax Amount"
                          type="number"
                          value={line.taxAmount || ''}
                          onChange={(e) => updateBillLine(index, 'taxAmount', parseFloat(e.target.value) || 0)}
                          inputProps={{ min: 0, step: 0.01 }}
                        />
                      </Grid>
                    </Grid>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateDialogOpen(false)} disabled={creating}>
            Cancel
          </Button>
          <Button
            onClick={handleCreateBill}
            variant="contained"
            disabled={creating}
          >
            {creating ? <CircularProgress size={24} /> : 'Create Bill'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default BillEntries;

