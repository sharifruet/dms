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
} from '@mui/material';
import {
  Search as SearchIcon,
  Receipt as BillIcon,
  FilterList as FilterIcon,
} from '@mui/icons-material';
import { financeService, BillHeader, BillLine } from '../services/financeService';

const BillEntries: React.FC = () => {
  const [bills, setBills] = useState<BillHeader[]>([]);
  const [selectedBill, setSelectedBill] = useState<BillHeader | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [fiscalYearFilter, setFiscalYearFilter] = useState<number | ''>('');
  const [vendorFilter, setVendorFilter] = useState('');

  useEffect(() => {
    loadBills();
  }, [fiscalYearFilter, vendorFilter]);

  const loadBills = async () => {
    try {
      setLoading(true);
      setError(null);
      const billsData = await financeService.getBills(
        fiscalYearFilter ? Number(fiscalYearFilter) : undefined,
        vendorFilter || undefined
      );
      setBills(billsData);
      // Load full details for selected bill if it exists
      if (selectedBill) {
        const updatedBill = billsData.find(b => b.id === selectedBill.id);
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
    </Box>
  );
};

export default BillEntries;

