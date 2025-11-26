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
  LinearProgress,
  FormControlLabel,
  Checkbox,
} from '@mui/material';
import {
  Upload as UploadIcon,
  Receipt as BillIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
  CloudUpload as CloudUploadIcon,
  Warning as WarningIcon,
  CheckCircle as CheckCircleIcon,
} from '@mui/icons-material';
import {
  financeService,
  BillHeader,
  BillLine,
  BillOCRResult,
  BillLineItemOCR,
} from '../services/financeService';

const BillEntries: React.FC = () => {
  const [bills, setBills] = useState<BillHeader[]>([]);
  const [selectedBill, setSelectedBill] = useState<BillHeader | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [fiscalYearFilter, setFiscalYearFilter] = useState<number | ''>('');
  const [vendorFilter, setVendorFilter] = useState('');
  
  // Upload & OCR states
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const [verificationDialogOpen, setVerificationDialogOpen] = useState(false);
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [extractingOCR, setExtractingOCR] = useState(false);
  const [ocrResult, setOcrResult] = useState<BillOCRResult | null>(null);
  const [ocrError, setOcrError] = useState<string | null>(null);
  
  // Verified/corrected data
  const [verifiedData, setVerifiedData] = useState<{
    vendorName: string;
    invoiceNumber: string;
    invoiceDate: string;
    fiscalYear: number;
    totalAmount: number;
    taxAmount: number;
    subtotalAmount: number;
    lineItems: Array<{
      projectIdentifier?: string;
      department?: string;
      costCenter?: string;
      category?: string;
      description?: string;
      amount: number;
      taxAmount?: number;
    }>;
  } | null>(null);
  
  const [saving, setSaving] = useState(false);

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
      if (selectedBill) {
        const updatedBill = billsArray.find(b => b.id === selectedBill.id);
        if (updatedBill) {
          loadBillDetails(updatedBill.id);
        }
      }
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to load bills');
    } finally {
      setLoading(false);
    }
  };

  const loadBillDetails = async (billId: number) => {
    try {
      const bill = await financeService.getBill(billId);
      setSelectedBill(bill);
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to load bill details');
    }
  };

  const handleFileUpload = async (file: File) => {
    // Validate file type
    const validTypes = ['image/jpeg', 'image/png', 'image/tiff', 'application/pdf'];
    if (!validTypes.includes(file.type)) {
      setOcrError('Only image files (JPEG, PNG, TIFF) or PDF files are allowed');
      return;
    }

    setUploadFile(file);
    setOcrError(null);
    setExtractingOCR(true);
    setOcrResult(null);

    try {
      const result = await financeService.extractBillOCR(file);
      setOcrResult(result);
      
      if (!result.success) {
        setOcrError(result.errorMessage || 'OCR extraction failed. You can enter bill data manually.');
        // Still allow manual entry
        initializeManualEntry();
        return;
      }

      // Initialize verified data from OCR result
      const initialVerifiedData = {
        vendorName: result.vendorName || '',
        invoiceNumber: result.invoiceNumber || '',
        invoiceDate: result.invoiceDate || new Date().toISOString().split('T')[0],
        fiscalYear: result.fiscalYear || new Date().getFullYear(),
        totalAmount: result.totalAmount ? Number(result.totalAmount) : 0,
        taxAmount: result.taxAmount ? Number(result.taxAmount) : 0,
        subtotalAmount: result.subtotalAmount ? Number(result.subtotalAmount) : 0,
        lineItems: result.lineItems && result.lineItems.length > 0
          ? result.lineItems.map((item: BillLineItemOCR) => ({
              projectIdentifier: item.projectIdentifier || '',
              department: item.department || '',
              costCenter: item.costCenter || '',
              category: item.category || '',
              description: item.description || '',
              amount: item.amount ? Number(item.amount) : 0,
              taxAmount: item.taxAmount ? Number(item.taxAmount) : 0,
            }))
          : result.totalAmount
          ? [{
              projectIdentifier: '',
              department: '',
              costCenter: '',
              category: '',
              description: '',
              amount: Number(result.totalAmount) - (result.taxAmount ? Number(result.taxAmount) : 0),
              taxAmount: result.taxAmount ? Number(result.taxAmount) : 0,
            }]
          : [{
              projectIdentifier: '',
              department: '',
              costCenter: '',
              category: '',
              description: '',
              amount: 0,
              taxAmount: 0,
            }],
      };
      
      setVerifiedData(initialVerifiedData);
      setUploadDialogOpen(false);
      setVerificationDialogOpen(true);
    } catch (err: any) {
      setOcrError(err.response?.data?.error || 'Failed to extract bill information. You can enter data manually.');
      initializeManualEntry();
    } finally {
      setExtractingOCR(false);
    }
  };

  const initializeManualEntry = () => {
    setVerifiedData({
      vendorName: '',
      invoiceNumber: '',
      invoiceDate: new Date().toISOString().split('T')[0],
      fiscalYear: new Date().getFullYear(),
      totalAmount: 0,
      taxAmount: 0,
      subtotalAmount: 0,
      lineItems: [{
        projectIdentifier: '',
        department: '',
        costCenter: '',
        category: '',
        description: '',
        amount: 0,
        taxAmount: 0,
      }],
    });
    setUploadDialogOpen(false);
    setVerificationDialogOpen(true);
  };

  const handleSaveVerifiedBill = async () => {
    if (!verifiedData) return;

    // Validate required fields
    if (!verifiedData.fiscalYear || !verifiedData.invoiceDate) {
      setError('Fiscal Year and Invoice Date are required');
      return;
    }

    if (!verifiedData.lineItems || verifiedData.lineItems.length === 0) {
      setError('At least one line item is required');
      return;
    }

    if (verifiedData.lineItems.some(line => !line.amount || line.amount <= 0)) {
      setError('All line items must have a positive amount');
      return;
    }

    try {
      setSaving(true);
      setError(null);

      const billData = {
        fiscalYear: verifiedData.fiscalYear,
        vendor: verifiedData.vendorName || undefined,
        invoiceNumber: verifiedData.invoiceNumber || undefined,
        invoiceDate: verifiedData.invoiceDate,
        lines: verifiedData.lineItems.map(line => ({
          projectIdentifier: line.projectIdentifier || undefined,
          department: line.department || undefined,
          costCenter: line.costCenter || undefined,
          category: line.category || undefined,
          amount: line.amount,
          taxAmount: line.taxAmount || undefined,
        })),
      };

      const billId = await financeService.createBill(billData);
      
      setVerificationDialogOpen(false);
      setUploadFile(null);
      setOcrResult(null);
      setVerifiedData(null);
      
      await loadBills();
      await loadBillDetails(billId);
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to save bill');
    } finally {
      setSaving(false);
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

  const calculateTotal = (lines?: BillLine[]): number => {
    if (!lines) return 0;
    return lines.reduce((sum, line) => sum + (line.amount || 0), 0);
  };

  const calculateTaxTotal = (lines?: BillLine[]): number => {
    if (!lines) return 0;
    return lines.reduce((sum, line) => sum + (line.taxAmount || 0), 0);
  };

  const getConfidenceColor = (confidence?: number): 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning' => {
    if (!confidence) return 'default';
    if (confidence >= 0.8) return 'success';
    if (confidence >= 0.6) return 'warning';
    return 'error';
  };

  const getConfidenceLabel = (confidence?: number): string => {
    if (!confidence) return 'Unknown';
    if (confidence >= 0.8) return 'High';
    if (confidence >= 0.6) return 'Medium';
    return 'Low';
  };

  const uniqueFiscalYears = Array.from(
    new Set(bills.map(b => b.fiscalYear).filter(y => y != null))
  ).sort((a, b) => b - a);

  const uniqueVendors = Array.from(
    new Set(bills.map(b => b.vendor).filter(v => v != null && v.trim() !== ''))
  ).sort();

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
          startIcon={<UploadIcon />}
          onClick={() => {
            setUploadDialogOpen(true);
            setOcrError(null);
            setOcrResult(null);
            setUploadFile(null);
          }}
        >
          Upload Bill
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

              {bills.length === 0 ? (
                <Alert severity="info">No bills found.</Alert>
              ) : (
                <Box sx={{ maxHeight: 600, overflow: 'auto' }}>
                  {bills.map((bill) => (
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

        {/* Bill Details */}
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

      {/* Upload Bill Dialog */}
      <Dialog open={uploadDialogOpen} onClose={() => !extractingOCR && setUploadDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Upload Bill (Image/PDF)</DialogTitle>
        <DialogContent>
          {extractingOCR && (
            <Box sx={{ mb: 2 }}>
              <Alert severity="info" sx={{ mb: 2 }}>
                Extracting information from bill using OCR...
              </Alert>
              <LinearProgress />
            </Box>
          )}

          {ocrError && (
            <Alert severity="warning" sx={{ mb: 2 }}>
              {ocrError}
            </Alert>
          )}

          <Box
            sx={{
              border: '2px dashed',
              borderColor: 'divider',
              borderRadius: 2,
              p: 4,
              textAlign: 'center',
              mb: 2,
            }}
          >
            <CloudUploadIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
            <Typography variant="body1" gutterBottom>
              {uploadFile ? uploadFile.name : 'Drop bill file here or click to browse'}
            </Typography>
            <Typography variant="caption" color="text.secondary" sx={{ mb: 2, display: 'block' }}>
              Supported formats: JPEG, PNG, TIFF, PDF
            </Typography>
            <Button
              variant="outlined"
              component="label"
              disabled={extractingOCR}
              startIcon={<UploadIcon />}
            >
              Choose File
              <input
                type="file"
                hidden
                accept="image/jpeg,image/png,image/tiff,application/pdf"
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file) {
                    handleFileUpload(file);
                  }
                }}
              />
            </Button>
          </Box>

          {uploadFile && (
            <Alert severity="info">
              Selected: {uploadFile.name} ({(uploadFile.size / 1024 / 1024).toFixed(2)} MB)
            </Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setUploadDialogOpen(false)} disabled={extractingOCR}>
            Cancel
          </Button>
          {uploadFile && !extractingOCR && (
            <Button
              variant="contained"
              onClick={() => handleFileUpload(uploadFile)}
            >
              Extract Information
            </Button>
          )}
        </DialogActions>
      </Dialog>

      {/* Verification Dialog - This will be shown after OCR extraction or for manual entry */}
      {verificationDialogOpen && verifiedData && (
        <BillVerificationDialog
          open={verificationDialogOpen}
          onClose={() => {
            setVerificationDialogOpen(false);
            setVerifiedData(null);
            setOcrResult(null);
            setUploadFile(null);
          }}
          ocrResult={ocrResult}
          verifiedData={verifiedData}
          onVerifiedDataChange={setVerifiedData}
          onSave={handleSaveVerifiedBill}
          saving={saving}
        />
      )}
    </Box>
  );
};

// Verified Bill Data Type
type VerifiedBillData = {
  vendorName: string;
  invoiceNumber: string;
  invoiceDate: string;
  fiscalYear: number;
  totalAmount: number;
  taxAmount: number;
  subtotalAmount: number;
  lineItems: Array<{
    projectIdentifier?: string;
    department?: string;
    costCenter?: string;
    category?: string;
    description?: string;
    amount: number;
    taxAmount?: number;
  }>;
};

// Bill Verification Dialog Component
interface BillVerificationDialogProps {
  open: boolean;
  onClose: () => void;
  ocrResult: BillOCRResult | null;
  verifiedData: VerifiedBillData;
  onVerifiedDataChange: (data: VerifiedBillData) => void;
  onSave: () => void;
  saving: boolean;
}

const BillVerificationDialog: React.FC<BillVerificationDialogProps> = ({
  open,
  onClose,
  ocrResult,
  verifiedData,
  onVerifiedDataChange,
  saving,
  onSave,
}) => {
  const [showOriginalValues, setShowOriginalValues] = useState(false);

  const getConfidenceLabel = (confidence?: number): string => {
    if (!confidence) return 'Unknown';
    if (confidence >= 0.8) return 'High';
    if (confidence >= 0.6) return 'Medium';
    return 'Low';
  };

  const getFieldConfidence = (fieldName: string): number | undefined => {
    if (!ocrResult) return undefined;
    switch (fieldName) {
      case 'vendorName':
        return ocrResult.vendorNameConfidence;
      case 'invoiceNumber':
        return ocrResult.invoiceNumberConfidence;
      case 'invoiceDate':
        return ocrResult.invoiceDateConfidence;
      case 'fiscalYear':
        return ocrResult.fiscalYearConfidence;
      case 'totalAmount':
        return ocrResult.totalAmountConfidence;
      case 'taxAmount':
        return ocrResult.taxAmountConfidence;
      default:
        return undefined;
    }
  };

  const getOriginalValue = (fieldName: string): string | number | undefined => {
    if (!ocrResult || !ocrResult.originalValues) return undefined;
    const values = ocrResult.originalValues as Record<string, string | number | undefined>;
    return values[fieldName];
  };

  const isFieldModified = (fieldName: string): boolean => {
    if (!ocrResult) return false;
    const original = getOriginalValue(fieldName);
    if (original === undefined) return false;
    
    switch (fieldName) {
      case 'vendorName':
        return verifiedData.vendorName !== original;
      case 'invoiceNumber':
        return verifiedData.invoiceNumber !== original;
      case 'invoiceDate':
        return verifiedData.invoiceDate !== original?.toString();
      case 'fiscalYear':
        return verifiedData.fiscalYear !== original;
      case 'totalAmount':
        return verifiedData.totalAmount !== original;
      case 'taxAmount':
        return verifiedData.taxAmount !== original;
      default:
        return false;
    }
  };

  const updateLineItem = (index: number, field: string, value: any) => {
    const updatedLineItems = [...verifiedData.lineItems];
    updatedLineItems[index] = { ...updatedLineItems[index], [field]: value };
    onVerifiedDataChange({ ...verifiedData, lineItems: updatedLineItems });
  };

  const addLineItem = () => {
    onVerifiedDataChange({
      ...verifiedData,
      lineItems: [
        ...verifiedData.lineItems,
        {
          projectIdentifier: '',
          department: '',
          costCenter: '',
          category: '',
          description: '',
          amount: 0,
          taxAmount: 0,
        },
      ],
    });
  };

  const removeLineItem = (index: number) => {
    if (verifiedData.lineItems.length > 1) {
      const updatedLineItems = verifiedData.lineItems.filter((_, i) => i !== index);
      onVerifiedDataChange({ ...verifiedData, lineItems: updatedLineItems });
    }
  };

  const calculateLineItemTotal = (index: number): number => {
    const line = verifiedData.lineItems[index];
    return (line.amount || 0) + (line.taxAmount || 0);
  };

  const calculateGrandTotal = (): number => {
    return verifiedData.lineItems.reduce((sum, line) => sum + calculateLineItemTotal(verifiedData.lineItems.indexOf(line)), 0);
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="lg" fullWidth>
      <DialogTitle>
        {ocrResult ? 'Verify & Correct Bill Information' : 'Enter Bill Information Manually'}
      </DialogTitle>
      <DialogContent>
        <Box sx={{ mt: 2 }}>
          {ocrResult && (
            <Alert severity="info" sx={{ mb: 2 }}>
              OCR extraction completed with {Math.round((ocrResult.overallConfidence || 0) * 100)}% overall confidence.
              Please review and correct the extracted information below.
            </Alert>
          )}

          {ocrResult && (
            <FormControlLabel
              control={
                <Checkbox
                  checked={showOriginalValues}
                  onChange={(e) => setShowOriginalValues(e.target.checked)}
                />
              }
              label="Show original OCR values"
              sx={{ mb: 2 }}
            />
          )}

          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Vendor Name"
                value={verifiedData.vendorName}
                onChange={(e) => onVerifiedDataChange({ ...verifiedData, vendorName: e.target.value })}
                error={ocrResult ? ((getFieldConfidence('vendorName') ?? 0) < 0.6) : false}
                helperText={
                  ocrResult && getFieldConfidence('vendorName') !== undefined
                    ? `Confidence: ${getConfidenceLabel(getFieldConfidence('vendorName'))} (${Math.round((getFieldConfidence('vendorName') || 0) * 100)}%)`
                    : ''
                }
                InputProps={{
                  endAdornment: ocrResult && isFieldModified('vendorName') && (
                    <Chip label="Modified" size="small" color="warning" />
                  ),
                }}
              />
              {showOriginalValues && getOriginalValue('vendorName') && (
                <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
                  Original: {getOriginalValue('vendorName')}
                </Typography>
              )}
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Invoice Number"
                value={verifiedData.invoiceNumber}
                onChange={(e) => onVerifiedDataChange({ ...verifiedData, invoiceNumber: e.target.value })}
                error={ocrResult ? ((getFieldConfidence('invoiceNumber') ?? 0) < 0.6) : false}
                helperText={
                  ocrResult && getFieldConfidence('invoiceNumber') !== undefined
                    ? `Confidence: ${getConfidenceLabel(getFieldConfidence('invoiceNumber'))} (${Math.round((getFieldConfidence('invoiceNumber') || 0) * 100)}%)`
                    : ''
                }
                InputProps={{
                  endAdornment: ocrResult && isFieldModified('invoiceNumber') && (
                    <Chip label="Modified" size="small" color="warning" />
                  ),
                }}
              />
              {showOriginalValues && getOriginalValue('invoiceNumber') && (
                <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
                  Original: {getOriginalValue('invoiceNumber')}
                </Typography>
              )}
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                required
                label="Invoice Date"
                type="date"
                value={verifiedData.invoiceDate}
                onChange={(e) => onVerifiedDataChange({ ...verifiedData, invoiceDate: e.target.value })}
                InputLabelProps={{ shrink: true }}
                error={ocrResult ? ((getFieldConfidence('invoiceDate') ?? 0) < 0.6) : false}
                helperText={
                  ocrResult && getFieldConfidence('invoiceDate') !== undefined
                    ? `Confidence: ${getConfidenceLabel(getFieldConfidence('invoiceDate'))} (${Math.round((getFieldConfidence('invoiceDate') || 0) * 100)}%)`
                    : ''
                }
                InputProps={{
                  endAdornment: ocrResult && isFieldModified('invoiceDate') && (
                    <Chip label="Modified" size="small" color="warning" />
                  ),
                }}
              />
              {showOriginalValues && getOriginalValue('invoiceDate') && (
                <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
                  Original: {getOriginalValue('invoiceDate')}
                </Typography>
              )}
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                required
                label="Fiscal Year"
                type="number"
                value={verifiedData.fiscalYear}
                onChange={(e) => onVerifiedDataChange({ ...verifiedData, fiscalYear: parseInt(e.target.value) || new Date().getFullYear() })}
                error={ocrResult ? ((getFieldConfidence('fiscalYear') ?? 0) < 0.6) : false}
                helperText={
                  ocrResult && getFieldConfidence('fiscalYear') !== undefined
                    ? `Confidence: ${getConfidenceLabel(getFieldConfidence('fiscalYear'))} (${Math.round((getFieldConfidence('fiscalYear') || 0) * 100)}%)`
                    : ''
                }
                InputProps={{
                  endAdornment: ocrResult && isFieldModified('fiscalYear') && (
                    <Chip label="Modified" size="small" color="warning" />
                  ),
                }}
              />
              {showOriginalValues && getOriginalValue('fiscalYear') && (
                <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
                  Original: {getOriginalValue('fiscalYear')}
                </Typography>
              )}
            </Grid>

            <Grid item xs={12}>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                <Typography variant="h6">Line Items</Typography>
                <Button size="small" onClick={addLineItem} startIcon={<UploadIcon />}>
                  Add Line
                </Button>
              </Box>
            </Grid>

            {verifiedData.lineItems.map((line, index) => (
              <Grid item xs={12} key={index}>
                <Card variant="outlined">
                  <CardContent>
                    <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                      <Typography variant="subtitle2">Line Item {index + 1}</Typography>
                      {verifiedData.lineItems.length > 1 && (
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => removeLineItem(index)}
                        >
                          <DeleteIcon fontSize="small" />
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
                          onChange={(e) => updateLineItem(index, 'projectIdentifier', e.target.value)}
                        />
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <TextField
                          fullWidth
                          size="small"
                          label="Department"
                          value={line.department || ''}
                          onChange={(e) => updateLineItem(index, 'department', e.target.value)}
                        />
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <TextField
                          fullWidth
                          size="small"
                          label="Cost Center"
                          value={line.costCenter || ''}
                          onChange={(e) => updateLineItem(index, 'costCenter', e.target.value)}
                        />
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <TextField
                          fullWidth
                          size="small"
                          label="Category"
                          value={line.category || ''}
                          onChange={(e) => updateLineItem(index, 'category', e.target.value)}
                        />
                      </Grid>
                      <Grid item xs={12}>
                        <TextField
                          fullWidth
                          size="small"
                          label="Description"
                          value={line.description || ''}
                          onChange={(e) => updateLineItem(index, 'description', e.target.value)}
                          multiline
                          rows={2}
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
                          onChange={(e) => updateLineItem(index, 'amount', parseFloat(e.target.value) || 0)}
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
                          onChange={(e) => updateLineItem(index, 'taxAmount', parseFloat(e.target.value) || 0)}
                          inputProps={{ min: 0, step: 0.01 }}
                        />
                      </Grid>
                      <Grid item xs={12}>
                        <Typography variant="body2" color="text.secondary">
                          Line Total: {new Intl.NumberFormat('en-US', {
                            style: 'currency',
                            currency: 'BDT',
                          }).format(calculateLineItemTotal(index))}
                        </Typography>
                      </Grid>
                    </Grid>
                  </CardContent>
                </Card>
              </Grid>
            ))}

            <Grid item xs={12}>
              <Box display="flex" justifyContent="space-between" alignItems="center" p={2} sx={{ bgcolor: 'action.selected', borderRadius: 1 }}>
                <Typography variant="subtitle1" fontWeight="bold">
                  Grand Total
                </Typography>
                <Typography variant="h6" fontWeight="bold">
                  {new Intl.NumberFormat('en-US', {
                    style: 'currency',
                    currency: 'BDT',
                  }).format(calculateGrandTotal())}
                </Typography>
              </Box>
            </Grid>
          </Grid>
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={saving}>
          Cancel
        </Button>
        <Button
          onClick={onSave}
          variant="contained"
          disabled={saving}
          startIcon={saving ? <CircularProgress size={20} /> : <CheckCircleIcon />}
        >
          {saving ? 'Saving...' : 'Save Bill'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default BillEntries;
