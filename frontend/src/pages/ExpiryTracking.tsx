import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Grid,
  Alert,
  CircularProgress,
  Tabs,
  Tab,
  Pagination,
  Tooltip,
  Menu,
  ListItemIcon,
  ListItemText
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  MoreVert as MoreVertIcon,
  Warning as WarningIcon,
  CheckCircle as CheckCircleIcon,
  Error as ErrorIcon,
  Schedule as ScheduleIcon,
  Assignment as AssignmentIcon,
  Business as BusinessIcon,
  AttachMoney as AttachMoneyIcon,
  CalendarToday as CalendarTodayIcon,
  Refresh as RefreshIcon
} from '@mui/icons-material';
import expiryTrackingService, { 
  ExpiryTracking, 
  ExpiryType, 
  ExpiryStatus, 
  CreateExpiryTrackingRequest,
  RenewExpiryTrackingRequest 
} from '../services/expiryTrackingService';
import { workflowService, WorkflowInstance } from '../services/workflowService';
import { documentService, Document } from '../services/documentService';
import { folderService } from '../services/folderService';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`expiry-tabpanel-${index}`}
      aria-labelledby={`expiry-tab-${index}`}
      {...other}
    >
      {value === index && (
        <Box sx={{ p: 3 }}>
          {children}
        </Box>
      )}
    </div>
  );
}

const ExpiryTrackingPage: React.FC = () => {
  const [trackingData, setTrackingData] = useState<ExpiryTracking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [tabValue, setTabValue] = useState(0);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [renewDialogOpen, setRenewDialogOpen] = useState(false);
  const [selectedTracking, setSelectedTracking] = useState<ExpiryTracking | null>(null);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [statistics, setStatistics] = useState<any>({});
  const [performanceSecurityDocs, setPerformanceSecurityDocs] = useState<ExpiryTracking[]>([]);
  const [loadingPSDocs, setLoadingPSDocs] = useState(false);
  
  const [newTracking, setNewTracking] = useState<CreateExpiryTrackingRequest>({
    documentId: 0,
    expiryType: ExpiryType.CONTRACT,
    expiryDate: '',
    notes: ''
  });

  // Workflow and document selection states
  const [workflows, setWorkflows] = useState<WorkflowInstance[]>([]);
  const [selectedWorkflowId, setSelectedWorkflowId] = useState<number | ''>('');
  const [workflowDocuments, setWorkflowDocuments] = useState<Document[]>([]);
  const [selectedDocumentId, setSelectedDocumentId] = useState<number | ''>('');
  const [selectedDocument, setSelectedDocument] = useState<Document | null>(null);
  const [loadingWorkflows, setLoadingWorkflows] = useState(false);
  const [loadingDocuments, setLoadingDocuments] = useState(false);
  const [existingExpiryDate, setExistingExpiryDate] = useState<string>('');

  const [renewRequest, setRenewRequest] = useState<RenewExpiryTrackingRequest>({
    newExpiryDate: '',
    renewalDocumentId: undefined,
    notes: ''
  });

  useEffect(() => {
    loadPerformanceSecurityDocuments();
  }, []);

  useEffect(() => {
    loadTrackingData();
    loadStatistics();
  }, [currentPage, tabValue, performanceSecurityDocs]);

  const loadPerformanceSecurityDocuments = async () => {
    try {
      setLoadingPSDocs(true);
      const docs = await expiryTrackingService.getPerformanceSecurityDocuments();
      setPerformanceSecurityDocs(docs);
    } catch (error) {
      console.error('Failed to load Performance Security documents:', error);
    } finally {
      setLoadingPSDocs(false);
    }
  };

  const loadTrackingData = async () => {
    try {
      setLoading(true);
      let response;
      let combinedData: ExpiryTracking[] = [];
      
      switch (tabValue) {
        case 0: // Active
          response = await expiryTrackingService.getActiveExpiryTracking(currentPage, 20);
          combinedData = [...(response.content || [])];
          // Add Performance Security documents that are active
          const activePSDocs = performanceSecurityDocs.filter(doc => {
            const days = getDaysUntilExpiry(doc.expiryDate);
            return days >= 0; // Not expired
          });
          combinedData = [...combinedData, ...activePSDocs];
          setTrackingData(combinedData);
          setTotalPages(response.totalPages || 0);
          return;
        case 1: // Expiring (30 days)
          response = await expiryTrackingService.getExpiringDocuments(30);
          combinedData = [...response];
          // Add Performance Security documents expiring in 30 days
          const expiringPSDocs = performanceSecurityDocs.filter(doc => {
            const days = getDaysUntilExpiry(doc.expiryDate);
            return days >= 0 && days <= 30;
          });
          combinedData = [...combinedData, ...expiringPSDocs];
          setTrackingData(combinedData);
          return;
        case 2: // Expired
          response = await expiryTrackingService.getExpiredDocuments();
          combinedData = [...response];
          // Add expired Performance Security documents
          const expiredPSDocs = performanceSecurityDocs.filter(doc => {
            const days = getDaysUntilExpiry(doc.expiryDate);
            return days < 0; // Expired
          });
          combinedData = [...combinedData, ...expiredPSDocs];
          setTrackingData(combinedData);
          return;
        default:
          response = await expiryTrackingService.getActiveExpiryTracking(currentPage, 20);
          combinedData = [...(response.content || [])];
          setTrackingData(combinedData);
          setTotalPages(response.totalPages || 0);
      }
    } catch (error) {
      setError('Failed to load expiry tracking data');
    } finally {
      setLoading(false);
    }
  };

  const loadStatistics = async () => {
    try {
      const stats = await expiryTrackingService.getExpiryStatistics();
      setStatistics(stats);
    } catch (error) {
      console.error('Failed to load statistics:', error);
    }
  };

  // Load workflows when dialog opens
  const loadWorkflows = async () => {
    try {
      setLoadingWorkflows(true);
      const response = await workflowService.getWorkflowInstances(0, 100);
      setWorkflows(response.content || []);
    } catch (error) {
      console.error('Failed to load workflows:', error);
      setError('Failed to load workflows');
    } finally {
      setLoadingWorkflows(false);
    }
  };

  // Load documents from selected workflow
  const loadDocumentsFromWorkflow = async (workflowId: number) => {
    try {
      setLoadingDocuments(true);
      setWorkflowDocuments([]);
      setSelectedDocumentId('');
      setSelectedDocument(null);
      setExistingExpiryDate('');

      // Find the workflow instance
      const workflowInstance = workflows.find(w => w.id === workflowId);
      if (!workflowInstance || !workflowInstance.workflow) {
        setError('Workflow not found');
        return;
      }

      // Get all workflow instances for this workflow to find documents
      const allInstances = await workflowService.getWorkflowInstances(0, 1000);
      const instancesForWorkflow = allInstances.content.filter(
        wi => wi.workflow.id === workflowInstance.workflow.id
      );

      // Collect unique documents from all instances
      const documentIds = new Set<number>();
      const documents: Document[] = [];

      for (const instance of instancesForWorkflow) {
        if (instance.document?.id && !documentIds.has(instance.document.id)) {
          documentIds.add(instance.document.id);
          try {
            const docResponse = await documentService.getDocumentById(instance.document.id);
            const doc = (docResponse as any).document || docResponse;
            documents.push(doc);
          } catch (err) {
            console.error(`Failed to load document ${instance.document.id}:`, err);
          }
        }
      }

      // If no documents from instances, try to get documents from workflow's folder
      if (documents.length === 0) {
        // Try to get folder from workflow - workflows have a folder relationship
        // For now, we'll use the workflow instance's document's folder as fallback
        if (workflowInstance.document?.id) {
          const docResponse = await documentService.getDocumentById(workflowInstance.document.id);
          const doc = (docResponse as any).document || docResponse;
          
          if (doc.folder?.id) {
            // Get all documents from this folder
            const docsResponse = await documentService.getDocuments({
              folderId: doc.folder.id,
              page: 0,
              size: 1000
            });
            setWorkflowDocuments(docsResponse.content || []);
            return;
          }
        }
        setError('No documents found for this workflow');
      } else {
        setWorkflowDocuments(documents);
      }
    } catch (error) {
      console.error('Failed to load documents from workflow:', error);
      setError('Failed to load documents');
    } finally {
      setLoadingDocuments(false);
    }
  };

  // Handle workflow selection
  const handleWorkflowChange = async (workflowId: number | '') => {
    setSelectedWorkflowId(workflowId);
    if (workflowId) {
      await loadDocumentsFromWorkflow(workflowId);
    } else {
      setWorkflowDocuments([]);
      setSelectedDocumentId('');
      setSelectedDocument(null);
      setExistingExpiryDate('');
    }
  };

  // Handle document selection
  const handleDocumentChange = async (documentId: number | '') => {
    setSelectedDocumentId(documentId);
    if (documentId) {
      try {
        const docResponse = await documentService.getDocumentById(documentId);
        const doc = (docResponse as any).document || docResponse;
        setSelectedDocument(doc);
        
        // Check for existing expiry date in metadata
        const metadata = (docResponse as any).metadata || {};
        const expiryDateFromMetadata = metadata.expiryDate || metadata.expiry_date || '';
        setExistingExpiryDate(expiryDateFromMetadata);
        
        // Pre-fill expiry date if it exists
        if (expiryDateFromMetadata) {
          setNewTracking({
            ...newTracking,
            documentId: documentId,
            expiryDate: expiryDateFromMetadata.split('T')[0] // Convert to date format
          });
        } else {
          setNewTracking({
            ...newTracking,
            documentId: documentId,
            expiryDate: ''
          });
        }
      } catch (error) {
        console.error('Failed to load document:', error);
        setError('Failed to load document details');
      }
    } else {
      setSelectedDocument(null);
      setExistingExpiryDate('');
      setNewTracking({
        ...newTracking,
        documentId: 0,
        expiryDate: ''
      });
    }
  };

  const handleCreateTracking = async () => {
    try {
      if (!selectedDocumentId || !newTracking.expiryDate) {
        setError('Please select a document and enter an expiry date');
        return;
      }

      // First, update document metadata with expiry date
      try {
        await documentService.updateDocumentMetadata(selectedDocumentId as number, {
          expiryDate: newTracking.expiryDate
        });
      } catch (metadataError) {
        console.error('Failed to update document metadata:', metadataError);
        // Continue anyway - metadata update is optional
      }

      // Then create expiry tracking
      await expiryTrackingService.createExpiryTracking({
        ...newTracking,
        documentId: selectedDocumentId as number
      });
      
      setCreateDialogOpen(false);
      setNewTracking({
        documentId: 0,
        expiryType: ExpiryType.CONTRACT,
        expiryDate: '',
        notes: ''
      });
      setSelectedWorkflowId('');
      setSelectedDocumentId('');
      setSelectedDocument(null);
      setWorkflowDocuments([]);
      setExistingExpiryDate('');
      await loadTrackingData();
      await loadStatistics();
    } catch (error) {
      setError('Failed to create expiry tracking');
    }
  };

  const handleRenewTracking = async () => {
    if (!selectedTracking) return;
    
    try {
      await expiryTrackingService.renewExpiryTracking(selectedTracking.id, renewRequest);
      setRenewDialogOpen(false);
      setRenewRequest({
        newExpiryDate: '',
        renewalDocumentId: undefined,
        notes: ''
      });
      await loadTrackingData();
      await loadStatistics();
    } catch (error) {
      setError('Failed to renew expiry tracking');
    }
  };

  const getStatusColor = (status: ExpiryStatus) => {
    switch (status) {
      case ExpiryStatus.ACTIVE:
        return 'success';
      case ExpiryStatus.EXPIRED:
        return 'error';
      case ExpiryStatus.RENEWED:
        return 'info';
      case ExpiryStatus.CANCELLED:
        return 'default';
      case ExpiryStatus.SUSPENDED:
        return 'warning';
      default:
        return 'default';
    }
  };

  const getStatusIcon = (status: ExpiryStatus) => {
    switch (status) {
      case ExpiryStatus.ACTIVE:
        return <CheckCircleIcon />;
      case ExpiryStatus.EXPIRED:
        return <ErrorIcon />;
      case ExpiryStatus.RENEWED:
        return <RefreshIcon />;
      case ExpiryStatus.CANCELLED:
        return <ErrorIcon />;
      case ExpiryStatus.SUSPENDED:
        return <WarningIcon />;
      default:
        return <ScheduleIcon />;
    }
  };

  const getDaysUntilExpiry = (expiryDate: string) => {
    const today = new Date();
    const expiry = new Date(expiryDate);
    const diffTime = expiry.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays;
  };

  const getExpiryAlertColor = (days: number) => {
    if (days < 0) return 'error';
    if (days <= 7) return 'error';
    if (days <= 15) return 'warning';
    if (days <= 30) return 'info';
    return 'success';
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString();
  };

  const formatCurrency = (amount?: number, currency?: string) => {
    if (!amount) return 'N/A';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency || 'BDT'
    }).format(amount);
  };

  if (loading && trackingData.length === 0) {
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
          Expiry Tracking
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setCreateDialogOpen(true)}
        >
          Add Tracking
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Statistics Cards */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center">
                <CheckCircleIcon color="success" sx={{ mr: 1 }} />
                <Box>
                  <Typography variant="h6">{statistics.active || 0}</Typography>
                  <Typography variant="body2" color="text.secondary">Active</Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center">
                <ErrorIcon color="error" sx={{ mr: 1 }} />
                <Box>
                  <Typography variant="h6">{statistics.expired || 0}</Typography>
                  <Typography variant="body2" color="text.secondary">Expired</Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center">
                <WarningIcon color="warning" sx={{ mr: 1 }} />
                <Box>
                  <Typography variant="h6">{statistics.expiringIn30Days || 0}</Typography>
                  <Typography variant="body2" color="text.secondary">Expiring in 30 days</Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center">
                <RefreshIcon color="info" sx={{ mr: 1 }} />
                <Box>
                  <Typography variant="h6">{statistics.renewed || 0}</Typography>
                  <Typography variant="body2" color="text.secondary">Renewed</Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Card>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)}>
            <Tab label="Active" />
            <Tab label="Expiring (30 days)" />
            <Tab label="Expired" />
          </Tabs>
        </Box>

        <TabPanel value={tabValue} index={0}>
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Document</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Expiry Date</TableCell>
                  <TableCell>Days Remaining</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Vendor</TableCell>
                  <TableCell>Value</TableCell>
                  <TableCell>Source</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {trackingData.map((tracking) => {
                  const daysRemaining = getDaysUntilExpiry(tracking.expiryDate);
                  return (
                    <TableRow key={tracking.id}>
                      <TableCell>
                        <Box display="flex" alignItems="center">
                          <AssignmentIcon sx={{ mr: 1 }} />
                          {(tracking as any).document?.originalName || 
                           (tracking as any).document?.fileName || 
                           `Document #${tracking.documentId}`}
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip label={tracking.expiryType.replace(/_/g, ' ')} size="small" />
                      </TableCell>
                      <TableCell>
                        <Box display="flex" alignItems="center">
                          <CalendarTodayIcon sx={{ mr: 1 }} />
                          {formatDate(tracking.expiryDate)}
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={`${daysRemaining} days`}
                          color={getExpiryAlertColor(daysRemaining)}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        <Chip
                          icon={getStatusIcon(tracking.status)}
                          label={tracking.status}
                          color={getStatusColor(tracking.status)}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        <Box display="flex" alignItems="center">
                          <BusinessIcon sx={{ mr: 1 }} />
                          {tracking.vendorName || 'N/A'}
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box display="flex" alignItems="center">
                          <AttachMoneyIcon sx={{ mr: 1 }} />
                          {formatCurrency(tracking.contractValue, tracking.currency)}
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={(tracking as any).isFromMetadata ? 'Metadata' : 'Tracking'}
                          size="small"
                          color={(tracking as any).isFromMetadata ? 'info' : 'default'}
                        />
                      </TableCell>
                      <TableCell>
                        <IconButton
                          onClick={(e) => {
                            setAnchorEl(e.currentTarget);
                            setSelectedTracking(tracking);
                          }}
                        >
                          <MoreVertIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>

          {totalPages > 1 && (
            <Box display="flex" justifyContent="center" mt={3}>
              <Pagination
                count={totalPages}
                page={currentPage + 1}
                onChange={(e, page) => setCurrentPage(page - 1)}
              />
            </Box>
          )}
        </TabPanel>

        <TabPanel value={tabValue} index={1}>
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Document</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Expiry Date</TableCell>
                  <TableCell>Days Remaining</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Vendor</TableCell>
                  <TableCell>Value</TableCell>
                  <TableCell>Source</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {trackingData.map((tracking) => {
                  const daysRemaining = getDaysUntilExpiry(tracking.expiryDate);
                  return (
                    <TableRow key={tracking.id}>
                      <TableCell>
                        <Box display="flex" alignItems="center">
                          <AssignmentIcon sx={{ mr: 1 }} />
                          {(tracking as any).document?.originalName || 
                           (tracking as any).document?.fileName || 
                           `Document #${tracking.documentId}`}
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip label={tracking.expiryType.replace(/_/g, ' ')} size="small" />
                      </TableCell>
                      <TableCell>
                        <Box display="flex" alignItems="center">
                          <CalendarTodayIcon sx={{ mr: 1 }} />
                          {formatDate(tracking.expiryDate)}
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={`${daysRemaining} days`}
                          color={getExpiryAlertColor(daysRemaining)}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        <Chip
                          icon={getStatusIcon(tracking.status)}
                          label={tracking.status}
                          color={getStatusColor(tracking.status)}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        <Box display="flex" alignItems="center">
                          <BusinessIcon sx={{ mr: 1 }} />
                          {tracking.vendorName || 'N/A'}
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box display="flex" alignItems="center">
                          <AttachMoneyIcon sx={{ mr: 1 }} />
                          {formatCurrency(tracking.contractValue, tracking.currency)}
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={(tracking as any).isFromMetadata ? 'Metadata' : 'Tracking'}
                          size="small"
                          color={(tracking as any).isFromMetadata ? 'info' : 'default'}
                        />
                      </TableCell>
                      <TableCell>
                        <IconButton
                          onClick={(e) => {
                            setAnchorEl(e.currentTarget);
                            setSelectedTracking(tracking);
                          }}
                        >
                          <MoreVertIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
        </TabPanel>

        <TabPanel value={tabValue} index={2}>
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Document</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Expiry Date</TableCell>
                  <TableCell>Days Overdue</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Vendor</TableCell>
                  <TableCell>Value</TableCell>
                  <TableCell>Source</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {trackingData.map((tracking) => {
                  const daysRemaining = getDaysUntilExpiry(tracking.expiryDate);
                  return (
                    <TableRow key={tracking.id}>
                      <TableCell>
                        <Box display="flex" alignItems="center">
                          <AssignmentIcon sx={{ mr: 1 }} />
                          {(tracking as any).document?.originalName || 
                           (tracking as any).document?.fileName || 
                           `Document #${tracking.documentId}`}
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip label={tracking.expiryType.replace(/_/g, ' ')} size="small" />
                      </TableCell>
                      <TableCell>
                        <Box display="flex" alignItems="center">
                          <CalendarTodayIcon sx={{ mr: 1 }} />
                          {formatDate(tracking.expiryDate)}
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={`${Math.abs(daysRemaining)} days ago`}
                          color="error"
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        <Chip
                          icon={getStatusIcon(tracking.status)}
                          label={tracking.status}
                          color={getStatusColor(tracking.status)}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        <Box display="flex" alignItems="center">
                          <BusinessIcon sx={{ mr: 1 }} />
                          {tracking.vendorName || 'N/A'}
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box display="flex" alignItems="center">
                          <AttachMoneyIcon sx={{ mr: 1 }} />
                          {formatCurrency(tracking.contractValue, tracking.currency)}
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={(tracking as any).isFromMetadata ? 'Metadata' : 'Tracking'}
                          size="small"
                          color={(tracking as any).isFromMetadata ? 'info' : 'default'}
                        />
                      </TableCell>
                      <TableCell>
                        <IconButton
                          onClick={(e) => {
                            setAnchorEl(e.currentTarget);
                            setSelectedTracking(tracking);
                          }}
                        >
                          <MoreVertIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
        </TabPanel>
      </Card>

      {/* Actions Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={() => setAnchorEl(null)}
      >
        <MenuItem onClick={() => {
          setRenewDialogOpen(true);
          setAnchorEl(null);
        }}>
          <ListItemIcon>
            <RefreshIcon />
          </ListItemIcon>
          <ListItemText>Renew</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => setAnchorEl(null)}>
          <ListItemIcon>
            <EditIcon />
          </ListItemIcon>
          <ListItemText>Edit</ListItemText>
        </MenuItem>
      </Menu>

      {/* Create Tracking Dialog */}
      <Dialog 
        open={createDialogOpen} 
        onClose={() => {
          setCreateDialogOpen(false);
          setSelectedWorkflowId('');
          setSelectedDocumentId('');
          setSelectedDocument(null);
          setWorkflowDocuments([]);
          setExistingExpiryDate('');
        }} 
        maxWidth="md" 
        fullWidth
      >
        <DialogTitle>Add Expiry Tracking</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            {/* Step 1: Select Workflow */}
            <Grid item xs={12}>
              <FormControl fullWidth>
                <InputLabel>Select Workflow</InputLabel>
                <Select
                  value={selectedWorkflowId}
                  onChange={(e) => handleWorkflowChange(e.target.value as number | '')}
                  onOpen={() => {
                    if (workflows.length === 0) {
                      loadWorkflows();
                    }
                  }}
                  disabled={loadingWorkflows}
                >
                  {loadingWorkflows && (
                    <MenuItem disabled>
                      <CircularProgress size={20} sx={{ mr: 1 }} />
                      Loading workflows...
                    </MenuItem>
                  )}
                  {!loadingWorkflows && workflows.length === 0 && (
                    <MenuItem disabled>No workflows available</MenuItem>
                  )}
                  {workflows.map((workflow) => (
                    <MenuItem key={workflow.id} value={workflow.id}>
                      {workflow.workflow.name} - {workflow.workflow.type}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>

            {/* Step 2: Select Document from Workflow */}
            {selectedWorkflowId && (
              <Grid item xs={12}>
                <FormControl fullWidth>
                  <InputLabel>Select Document</InputLabel>
                  <Select
                    value={selectedDocumentId}
                    onChange={(e) => handleDocumentChange(e.target.value as number | '')}
                    disabled={loadingDocuments || workflowDocuments.length === 0}
                  >
                    {loadingDocuments && (
                      <MenuItem disabled>
                        <CircularProgress size={20} sx={{ mr: 1 }} />
                        Loading documents...
                      </MenuItem>
                    )}
                    {!loadingDocuments && workflowDocuments.length === 0 && (
                      <MenuItem disabled>No documents found in this workflow</MenuItem>
                    )}
                    {workflowDocuments.map((doc) => (
                      <MenuItem key={doc.id} value={doc.id}>
                        {doc.originalName || doc.fileName}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
            )}

            {/* Show existing expiry date if present */}
            {existingExpiryDate && (
              <Grid item xs={12}>
                <Alert severity="info">
                  Existing expiry date: {existingExpiryDate.split('T')[0]}
                </Alert>
              </Grid>
            )}
            <Grid item xs={6}>
              <FormControl fullWidth>
                <InputLabel>Expiry Type</InputLabel>
                <Select
                  value={newTracking.expiryType}
                  onChange={(e) => setNewTracking({ ...newTracking, expiryType: e.target.value as ExpiryType })}
                >
                  {Object.values(ExpiryType).map((type) => (
                    <MenuItem key={type} value={type}>
                      {type.replace(/_/g, ' ')}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={6}>
              <TextField
                fullWidth
                label="Expiry Date"
                type="date"
                value={newTracking.expiryDate}
                onChange={(e) => setNewTracking({ ...newTracking, expiryDate: e.target.value })}
                InputLabelProps={{ shrink: true }}
                required
                disabled={!selectedDocumentId}
                helperText={!selectedDocumentId ? 'Please select a document first' : existingExpiryDate ? 'Updating existing expiry date' : 'Add expiry date'}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                rows={3}
                label="Notes"
                value={newTracking.notes}
                onChange={(e) => setNewTracking({ ...newTracking, notes: e.target.value })}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => {
            setCreateDialogOpen(false);
            setSelectedWorkflowId('');
            setSelectedDocumentId('');
            setSelectedDocument(null);
            setWorkflowDocuments([]);
            setExistingExpiryDate('');
          }}>Cancel</Button>
          <Button 
            onClick={handleCreateTracking} 
            variant="contained"
            disabled={!selectedDocumentId || !newTracking.expiryDate}
          >
            {existingExpiryDate ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Renew Dialog */}
      <Dialog open={renewDialogOpen} onClose={() => setRenewDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Renew Expiry Tracking</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={6}>
              <TextField
                fullWidth
                label="New Expiry Date"
                type="date"
                value={renewRequest.newExpiryDate}
                onChange={(e) => setRenewRequest({ ...renewRequest, newExpiryDate: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                fullWidth
                label="Renewal Document ID"
                type="number"
                value={renewRequest.renewalDocumentId || ''}
                onChange={(e) => setRenewRequest({ ...renewRequest, renewalDocumentId: parseInt(e.target.value) || undefined })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                rows={3}
                label="Renewal Notes"
                value={renewRequest.notes}
                onChange={(e) => setRenewRequest({ ...renewRequest, notes: e.target.value })}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRenewDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleRenewTracking} variant="contained">
            Renew
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default ExpiryTrackingPage;
