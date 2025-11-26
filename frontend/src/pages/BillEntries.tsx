import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Paper,
  Alert,
  CircularProgress,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Chip,
  LinearProgress,
  Divider,
  TextField,
  Link,
  Tooltip,
  IconButton,
} from '@mui/material';
import {
  Upload as UploadIcon,
  Receipt as BillIcon,
  Edit as EditIcon,
  CloudUpload as CloudUploadIcon,
  AccountTree as WorkflowIcon,
  TableChart as AppIcon,
  FilterList as FilterIcon,
  Clear as ClearIcon,
} from '@mui/icons-material';
import { documentService, Document, DocumentUploadRequest } from '../services/documentService';
import { DocumentType } from '../constants/documentTypes';
import { useAppSelector } from '../hooks/redux';
import BillFieldsEditor from '../components/BillFieldsEditor';
import DocumentViewer from '../components/DocumentViewer';
import { folderService } from '../services/folderService';
import { workflowService, Workflow, AppEntry } from '../services/workflowService';
import { useNavigate } from 'react-router-dom';

const BillEntries: React.FC = () => {
  const navigate = useNavigate();
  const [bills, setBills] = useState<Document[]>([]);
  const [selectedBill, setSelectedBill] = useState<Document | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  // Filter states
  const [fiscalYearFilter, setFiscalYearFilter] = useState<number | ''>('');
  const [vendorFilter, setVendorFilter] = useState('');
  const [invoiceNumberFilter, setInvoiceNumberFilter] = useState('');
  const [dateFromFilter, setDateFromFilter] = useState('');
  const [dateToFilter, setDateToFilter] = useState('');
  const [amountMinFilter, setAmountMinFilter] = useState('');
  const [amountMaxFilter, setAmountMaxFilter] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  
  // Workflow and APP entry states
  const [selectedBillWorkflow, setSelectedBillWorkflow] = useState<Workflow | null>(null);
  const [selectedBillAppEntry, setSelectedBillAppEntry] = useState<AppEntry | null>(null);
  const [loadingWorkflowInfo, setLoadingWorkflowInfo] = useState(false);
  
  // Upload states
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [extractingOCR, setExtractingOCR] = useState(false);
  const [ocrError, setOcrError] = useState<string | null>(null);
  const [viewerOpen, setViewerOpen] = useState(false);
  const { user } = useAppSelector((state) => state.auth);
  const [billMetadata, setBillMetadata] = useState<Record<string, string>>({});

  useEffect(() => {
    loadBills();
  }, [fiscalYearFilter, vendorFilter, invoiceNumberFilter, dateFromFilter, dateToFilter, amountMinFilter, amountMaxFilter]);

  const loadBills = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await documentService.getDocuments({
        documentType: DocumentType.BILL,
        searchTerm: vendorFilter || invoiceNumberFilter || undefined,
        page: 0,
        size: 1000, // Load more to allow client-side filtering
      });
      let billsArray = response.content || [];
      
      // Apply client-side filters for metadata-based fields
      if (fiscalYearFilter || dateFromFilter || dateToFilter || amountMinFilter || amountMaxFilter || invoiceNumberFilter) {
        billsArray = billsArray.filter(bill => {
          if (!bill.id) return false;
          const metadata = billMetadataCache.get(bill.id!) || {};
          const fiscalYear = getBillMetadataValue(metadata, 'fiscalYear');
          const invoiceDate = getBillMetadataValue(metadata, 'invoiceDate');
          const invoiceNumber = getBillMetadataValue(metadata, 'invoiceNumber').toLowerCase();
          const totalAmount = getBillMetadataNumber(metadata, 'totalAmount');
          
          // Fiscal year filter
          if (fiscalYearFilter && fiscalYear !== fiscalYearFilter.toString()) {
            return false;
          }
          
          // Invoice number filter
          if (invoiceNumberFilter && !invoiceNumber.includes(invoiceNumberFilter.toLowerCase())) {
            return false;
          }
          
          // Date range filter
          if (dateFromFilter || dateToFilter) {
            if (!invoiceDate) return false;
            const billDate = new Date(invoiceDate);
            if (dateFromFilter && billDate < new Date(dateFromFilter)) {
              return false;
            }
            if (dateToFilter) {
              const toDate = new Date(dateToFilter);
              toDate.setHours(23, 59, 59, 999); // Include full day
              if (billDate > toDate) {
                return false;
              }
            }
          }
          
          // Amount range filter
          if (amountMinFilter && totalAmount < parseFloat(amountMinFilter)) {
            return false;
          }
          if (amountMaxFilter && totalAmount > parseFloat(amountMaxFilter)) {
            return false;
          }
          
          return true;
        });
      }
      
      setBills(billsArray);
      if (selectedBill && selectedBill.id) {
        const updatedBill = billsArray.find(b => b.id === selectedBill.id);
        if (updatedBill && updatedBill.id) {
          await loadBillDetails(updatedBill.id!);
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
      const bill = await documentService.getDocumentById(billId);
      const doc = (bill as any).document || bill;
      const metadata = (bill as any).metadata || {};
      setSelectedBill(doc as Document);
      setBillMetadata(metadata);
      
      // Load workflow and APP entry info if bill is in a folder
      if (doc.folder?.id) {
        await loadWorkflowAndAppEntryInfo(doc.folder.id);
      } else {
        setSelectedBillWorkflow(null);
        setSelectedBillAppEntry(null);
      }
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to load bill details');
    }
  };

  const loadWorkflowAndAppEntryInfo = async (folderId: number) => {
    try {
      setLoadingWorkflowInfo(true);
      const workflow = await folderService.getFolderWorkflow(folderId);
      if (workflow) {
        setSelectedBillWorkflow(workflow);
        // Get APP entry for this workflow
        if (workflow.id) {
          const appEntryResponse = await workflowService.getAppEntryForWorkflow(workflow.id);
          if (appEntryResponse.success && appEntryResponse.appEntry) {
            setSelectedBillAppEntry(appEntryResponse.appEntry);
          } else {
            setSelectedBillAppEntry(null);
          }
        }
      } else {
        setSelectedBillWorkflow(null);
        setSelectedBillAppEntry(null);
      }
    } catch (err: any) {
      console.error('Failed to load workflow info:', err);
      setSelectedBillWorkflow(null);
      setSelectedBillAppEntry(null);
    } finally {
      setLoadingWorkflowInfo(false);
    }
  };

  const clearAllFilters = () => {
    setFiscalYearFilter('');
    setVendorFilter('');
    setInvoiceNumberFilter('');
    setDateFromFilter('');
    setDateToFilter('');
    setAmountMinFilter('');
    setAmountMaxFilter('');
  };

  const hasActiveFilters = () => {
    return !!(
      fiscalYearFilter ||
      vendorFilter ||
      invoiceNumberFilter ||
      dateFromFilter ||
      dateToFilter ||
      amountMinFilter ||
      amountMaxFilter
    );
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

    try {
      // Upload bill as a document - OCR extraction happens automatically on backend
      const uploadRequest: DocumentUploadRequest = {
        file: file,
        title: file.name,
        description: 'Bill/Invoice document',
        documentType: DocumentType.BILL,
        department: user?.department || '',
        userId: user?.id || 0,
      };

      const response = await documentService.uploadDocument(uploadRequest);
      
      if (response.success && response.documentId) {
        // Load the uploaded document
        await loadBills();
        await loadBillDetails(response.documentId);
        setUploadDialogOpen(false);
        setUploadFile(null);
        setError(null);
      } else {
        setOcrError(response.message || 'Failed to upload bill');
      }
    } catch (err: any) {
      setOcrError(err.response?.data?.error || 'Failed to upload bill document');
    } finally {
      setExtractingOCR(false);
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

  // Helper to extract bill metadata values
  const getBillMetadataValue = (metadata: Record<string, string>, key: string): string => {
    return metadata[key] || '';
  };

  const getBillMetadataNumber = (metadata: Record<string, string>, key: string): number => {
    const value = metadata[key];
    if (!value) return 0;
    try {
      return parseFloat(value);
    } catch {
      return 0;
    }
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

  // Extract unique fiscal years and vendors from bill metadata
  const [billMetadataCache, setBillMetadataCache] = useState<Map<number, Record<string, string>>>(new Map());

  useEffect(() => {
    // Load metadata for all bills in parallel
    const loadBillMetadata = async () => {
      const metadataMap = new Map<number, Record<string, string>>();
      const promises = bills.map(async (bill) => {
        if (!bill.id) return;
        try {
          const docData = await documentService.getDocumentById(bill.id!);
          const metadata = (docData as any).metadata || {};
          metadataMap.set(bill.id!, metadata);
        } catch (err) {
          // Ignore errors for individual bills
          metadataMap.set(bill.id!, {});
        }
      });
      await Promise.all(promises);
      setBillMetadataCache(metadataMap);
    };
    
    if (bills.length > 0) {
      loadBillMetadata();
    }
  }, [bills]);

  const uniqueFiscalYears = Array.from(
    new Set(
      Array.from(billMetadataCache.values())
        .map(meta => {
          const fy = meta.fiscalYear;
          return fy ? parseInt(fy) : null;
        })
        .filter((y): y is number => y != null)
    )
  ).sort((a, b) => b - a);

  const uniqueVendors = Array.from(
    new Set(
      Array.from(billMetadataCache.values())
        .map(meta => meta.vendorName)
        .filter(v => v != null && v.trim() !== '')
    )
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
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                <Typography variant="h6">
                  Bills ({bills.length})
                </Typography>
                <Box display="flex" gap={1}>
                  {hasActiveFilters() && (
                    <Tooltip title="Clear all filters">
                      <IconButton size="small" onClick={clearAllFilters}>
                        <ClearIcon />
                      </IconButton>
                    </Tooltip>
                  )}
                  <Tooltip title={showFilters ? 'Hide filters' : 'Show filters'}>
                    <IconButton size="small" onClick={() => setShowFilters(!showFilters)}>
                      <FilterIcon />
                    </IconButton>
                  </Tooltip>
                </Box>
              </Box>

              {showFilters && (
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
                  <Grid item xs={12} sm={6}>
                    <TextField
                      fullWidth
                      size="small"
                      label="Invoice Number"
                      value={invoiceNumberFilter}
                      onChange={(e) => setInvoiceNumberFilter(e.target.value)}
                      placeholder="Search by invoice number..."
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      fullWidth
                      size="small"
                      type="date"
                      label="Date From"
                      value={dateFromFilter}
                      onChange={(e) => setDateFromFilter(e.target.value)}
                      InputLabelProps={{ shrink: true }}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      fullWidth
                      size="small"
                      type="date"
                      label="Date To"
                      value={dateToFilter}
                      onChange={(e) => setDateToFilter(e.target.value)}
                      InputLabelProps={{ shrink: true }}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      fullWidth
                      size="small"
                      type="number"
                      label="Amount Min (BDT)"
                      value={amountMinFilter}
                      onChange={(e) => setAmountMinFilter(e.target.value)}
                      inputProps={{ min: 0, step: 0.01 }}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      fullWidth
                      size="small"
                      type="number"
                      label="Amount Max (BDT)"
                      value={amountMaxFilter}
                      onChange={(e) => setAmountMaxFilter(e.target.value)}
                      inputProps={{ min: 0, step: 0.01 }}
                    />
                  </Grid>
                </Grid>
              )}

              {bills.length === 0 ? (
                <Alert severity="info">No bills found.</Alert>
              ) : (
                <Box sx={{ maxHeight: 600, overflow: 'auto' }}>
                  {bills.map((bill, index) => {
                    if (!bill.id) return null;
                    const metadata = billMetadataCache.get(bill.id!) || {};
                    const invoiceNumber = getBillMetadataValue(metadata, 'invoiceNumber');
                    const vendorName = getBillMetadataValue(metadata, 'vendorName');
                    const fiscalYear = getBillMetadataValue(metadata, 'fiscalYear');
                    const invoiceDate = getBillMetadataValue(metadata, 'invoiceDate');
                    const totalAmount = getBillMetadataNumber(metadata, 'totalAmount');
                    
                    return (
                      <Card
                        key={bill.id ?? index}
                        sx={{
                          mb: 1,
                          cursor: 'pointer',
                          border: selectedBill?.id === bill.id ? 2 : 1,
                          borderColor: selectedBill?.id === bill.id ? 'primary.main' : 'divider',
                          '&:hover': {
                            backgroundColor: 'action.hover',
                          },
                        }}
                        onClick={() => bill.id && loadBillDetails(bill.id!)}
                      >
                        <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
                          <Box display="flex" alignItems="center" gap={1} mb={1}>
                            <BillIcon color="primary" />
                            <Typography variant="subtitle2" noWrap>
                              {invoiceNumber || `Bill #${bill.id}`}
                            </Typography>
                          </Box>
                          <Box display="flex" flexDirection="column" gap={0.5}>
                            {vendorName && (
                              <Typography variant="body2" color="text.secondary">
                                {vendorName}
                              </Typography>
                            )}
                            <Box display="flex" gap={1} flexWrap="wrap" alignItems="center">
                              {fiscalYear && (
                                <Chip label={`FY ${fiscalYear}`} size="small" />
                              )}
                              {invoiceDate && (
                                <Typography variant="caption" color="text.secondary">
                                  {formatDate(invoiceDate)}
                                </Typography>
                              )}
                              {totalAmount > 0 && (
                                <Typography variant="caption" color="primary" fontWeight="bold">
                                  {formatCurrency(totalAmount)}
                                </Typography>
                              )}
                            </Box>
                          </Box>
                        </CardContent>
                      </Card>
                    );
                  })}
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
                  ? `Bill Details: ${getBillMetadataValue(billMetadata, 'invoiceNumber') || `Bill #${selectedBill.id}`}`
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
              ) : (
                <>
                  {/* Bill Summary Fields */}
                  <Box mb={3}>
                    <Grid container spacing={2}>
                      <Grid item xs={12} sm={6}>
                        <Typography variant="body2" color="text.secondary">
                          Vendor Name
                        </Typography>
                        <Typography variant="body1" fontWeight={500}>
                          {getBillMetadataValue(billMetadata, 'vendorName') || '-'}
                        </Typography>
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <Typography variant="body2" color="text.secondary">
                          Invoice Number
                        </Typography>
                        <Typography variant="body1" fontWeight={500}>
                          {getBillMetadataValue(billMetadata, 'invoiceNumber') || '-'}
                        </Typography>
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <Typography variant="body2" color="text.secondary">
                          Invoice Date
                        </Typography>
                        <Typography variant="body1" fontWeight={500}>
                          {formatDate(getBillMetadataValue(billMetadata, 'invoiceDate'))}
                        </Typography>
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <Typography variant="body2" color="text.secondary">
                          Fiscal Year
                        </Typography>
                        <Typography variant="body1" fontWeight={500}>
                          {getBillMetadataValue(billMetadata, 'fiscalYear') || '-'}
                        </Typography>
                      </Grid>
                      <Grid item xs={12} sm={4}>
                        <Typography variant="body2" color="text.secondary">
                          Net Amount (BDT)
                        </Typography>
                        <Typography variant="body1" fontWeight={600} color="primary">
                          {formatCurrency(getBillMetadataNumber(billMetadata, 'netAmount'))}
                        </Typography>
                      </Grid>
                      <Grid item xs={12} sm={4}>
                        <Typography variant="body2" color="text.secondary">
                          Tax Amount (BDT)
                        </Typography>
                        <Typography variant="body1" fontWeight={600}>
                          {formatCurrency(getBillMetadataNumber(billMetadata, 'taxAmount'))}
                        </Typography>
                      </Grid>
                      <Grid item xs={12} sm={4}>
                        <Typography variant="body2" color="text.secondary">
                          Total Amount (BDT)
                        </Typography>
                        <Typography variant="h6" fontWeight={700} color="success.main">
                          {formatCurrency(getBillMetadataNumber(billMetadata, 'totalAmount'))}
                        </Typography>
                      </Grid>
                      {getBillMetadataValue(billMetadata, 'description') && (
                        <Grid item xs={12}>
                          <Typography variant="body2" color="text.secondary">
                            Description
                          </Typography>
                          <Typography variant="body1">
                            {getBillMetadataValue(billMetadata, 'description')}
                          </Typography>
                        </Grid>
                      )}
                    </Grid>
                  </Box>

                  {/* Workflow and APP Entry Information */}
                  {(selectedBillWorkflow || selectedBillAppEntry || loadingWorkflowInfo) && (
                    <>
                      <Divider sx={{ my: 3 }} />
                      <Box>
                        <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                          Workflow & Budget Information
                        </Typography>
                        {loadingWorkflowInfo ? (
                          <Box display="flex" justifyContent="center" p={2}>
                            <CircularProgress size={24} />
                          </Box>
                        ) : (
                          <Grid container spacing={2}>
                            {selectedBillWorkflow && (
                              <Grid item xs={12}>
                                <Card variant="outlined" sx={{ bgcolor: 'action.hover' }}>
                                  <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
                                    <Box display="flex" alignItems="center" gap={1} mb={1}>
                                      <WorkflowIcon color="primary" />
                                      <Typography variant="subtitle2" fontWeight={600}>
                                        Workflow: {selectedBillWorkflow.name}
                                      </Typography>
                                    </Box>
                                    {selectedBillWorkflow.description && (
                                      <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                                        {selectedBillWorkflow.description}
                                      </Typography>
                                    )}
                                    <Box display="flex" gap={1} flexWrap="wrap">
                                      <Chip label={selectedBillWorkflow.status} size="small" />
                                      <Chip label={selectedBillWorkflow.type} size="small" variant="outlined" />
                                      <Button
                                        size="small"
                                        variant="outlined"
                                        startIcon={<WorkflowIcon />}
                                        onClick={() => navigate(`/workflows?selected=${selectedBillWorkflow.id}`)}
                                      >
                                        View Workflow
                                      </Button>
                                    </Box>
                                  </CardContent>
                                </Card>
                              </Grid>
                            )}
                            {selectedBillAppEntry && (
                              <Grid item xs={12}>
                                <Card variant="outlined" sx={{ bgcolor: 'rgba(76, 175, 80, 0.1)' }}>
                                  <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
                                    <Box display="flex" alignItems="center" gap={1} mb={1}>
                                      <AppIcon color="success" />
                                      <Typography variant="subtitle2" fontWeight={600}>
                                        Linked Budget Entry (APP)
                                      </Typography>
                                    </Box>
                                    <Grid container spacing={1}>
                                      <Grid item xs={12} sm={6}>
                                        <Typography variant="body2" color="text.secondary">
                                          Fiscal Year
                                        </Typography>
                                        <Typography variant="body1" fontWeight={500}>
                                          FY {selectedBillAppEntry.fiscalYear}-{((selectedBillAppEntry.fiscalYear + 1) % 100).toString().padStart(2, '0')}
                                        </Typography>
                                      </Grid>
                                      <Grid item xs={12} sm={6}>
                                        <Typography variant="body2" color="text.secondary">
                                          Allocation Type
                                        </Typography>
                                        <Typography variant="body1" fontWeight={500}>
                                          {selectedBillAppEntry.allocationType || '-'}
                                        </Typography>
                                      </Grid>
                                      {selectedBillAppEntry.releaseInstallmentNo && (
                                        <Grid item xs={12} sm={6}>
                                          <Typography variant="body2" color="text.secondary">
                                            Installment
                                          </Typography>
                                          <Typography variant="body1" fontWeight={500}>
                                            {selectedBillAppEntry.releaseInstallmentNo}
                                          </Typography>
                                        </Grid>
                                      )}
                                      {selectedBillAppEntry.allocationAmount && (
                                        <Grid item xs={12} sm={6}>
                                          <Typography variant="body2" color="text.secondary">
                                            Allocation Amount
                                          </Typography>
                                          <Typography variant="body1" fontWeight={600} color="success.main">
                                            {formatCurrency(selectedBillAppEntry.allocationAmount)}
                                          </Typography>
                                        </Grid>
                                      )}
                                      {selectedBillAppEntry.referenceMemoNumber && (
                                        <Grid item xs={12}>
                                          <Typography variant="body2" color="text.secondary">
                                            Reference/Memo Number
                                          </Typography>
                                          <Typography variant="body1">
                                            {selectedBillAppEntry.referenceMemoNumber}
                                          </Typography>
                                        </Grid>
                                      )}
                                    </Grid>
                                    <Box mt={2}>
                                      <Button
                                        size="small"
                                        variant="outlined"
                                        color="success"
                                        startIcon={<AppIcon />}
                                        onClick={() => navigate(`/app-entries?selected=${selectedBillAppEntry.id}`)}
                                      >
                                        View APP Entry
                                      </Button>
                                    </Box>
                                  </CardContent>
                                </Card>
                              </Grid>
                            )}
                            {!selectedBillWorkflow && !selectedBillAppEntry && (
                              <Grid item xs={12}>
                                <Alert severity="info">
                                  This bill is not part of a workflow or linked to a budget entry.
                                </Alert>
                              </Grid>
                            )}
                          </Grid>
                        )}
                      </Box>
                    </>
                  )}

                  <Divider sx={{ my: 3 }} />

                  {/* Bill Fields Editor */}
                  <Box>
                    <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                      Edit Bill Fields
                    </Typography>
                    <Alert severity="info" sx={{ mb: 2 }}>
                      Edit the bill fields below. Fields extracted from OCR will show confidence scores.
                      You can verify and correct any extracted values.
                    </Alert>
                    <BillFieldsEditor
                      documentId={selectedBill.id!}
                      documentType={selectedBill.documentType}
                      metadata={billMetadata}
                      onSave={async (updatedMetadata) => {
                        if (!selectedBill.id) return;
                        try {
                          setError(null);
                          
                          // Save metadata to backend
                          await documentService.updateDocumentMetadata(selectedBill.id!, updatedMetadata);
                          
                          // Update local state immediately for responsive UI
                          setBillMetadata(updatedMetadata);
                          
                          // Update the metadata cache for this bill
                          setBillMetadataCache(prev => {
                            const updated = new Map(prev);
                            updated.set(selectedBill.id!, updatedMetadata);
                            return updated;
                          });
                          
                          // Reload document to refresh metadata and ensure consistency
                          await loadBillDetails(selectedBill.id!);
                          
                          // Reload bills list to refresh any filtered data
                          await loadBills();
                        } catch (err: any) {
                          const errorMessage = err.response?.data?.error || 'Failed to save bill fields';
                          setError(errorMessage);
                          throw err; // Re-throw so BillFieldsEditor can handle it and show its own error
                        }
                      }}
                    />
                  </Box>

                  {/* View Document Button */}
                  <Box mt={3}>
                    <Button
                      variant="outlined"
                      fullWidth
                      onClick={() => setViewerOpen(true)}
                      startIcon={<EditIcon />}
                    >
                      View Full Document
                    </Button>
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

      {/* Document Viewer */}
      <DocumentViewer
        open={viewerOpen}
        onClose={() => setViewerOpen(false)}
        document={selectedBill}
      />
    </Box>
  );
};

export default BillEntries;
