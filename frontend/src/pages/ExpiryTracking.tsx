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
  
  const [newTracking, setNewTracking] = useState<CreateExpiryTrackingRequest>({
    documentId: 0,
    expiryType: ExpiryType.CONTRACT,
    expiryDate: '',
    notes: ''
  });

  const [renewRequest, setRenewRequest] = useState<RenewExpiryTrackingRequest>({
    newExpiryDate: '',
    renewalDocumentId: undefined,
    notes: ''
  });

  useEffect(() => {
    loadTrackingData();
    loadStatistics();
  }, [currentPage, tabValue]);

  const loadTrackingData = async () => {
    try {
      setLoading(true);
      let response;
      
      switch (tabValue) {
        case 0: // Active
          response = await expiryTrackingService.getActiveExpiryTracking(currentPage, 20);
          break;
        case 1: // Expiring (30 days)
          response = await expiryTrackingService.getExpiringDocuments(30);
          setTrackingData(response);
          return;
        case 2: // Expired
          response = await expiryTrackingService.getExpiredDocuments();
          setTrackingData(response);
          return;
        default:
          response = await expiryTrackingService.getActiveExpiryTracking(currentPage, 20);
      }
      
      setTrackingData(response.content || response);
      setTotalPages(response.totalPages || 0);
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

  const handleCreateTracking = async () => {
    try {
      await expiryTrackingService.createExpiryTracking(newTracking);
      setCreateDialogOpen(false);
      setNewTracking({
        documentId: 0,
        expiryType: ExpiryType.CONTRACT,
        expiryDate: '',
        notes: ''
      });
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
      currency: currency || 'USD'
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
                          Document #{tracking.documentId}
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
          <Typography variant="h6" mb={2}>
            Documents Expiring in Next 30 Days
          </Typography>
          {/* Similar table structure for expiring documents */}
        </TabPanel>

        <TabPanel value={tabValue} index={2}>
          <Typography variant="h6" mb={2}>
            Expired Documents
          </Typography>
          {/* Similar table structure for expired documents */}
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
      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Add Expiry Tracking</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Document ID"
                type="number"
                value={newTracking.documentId}
                onChange={(e) => setNewTracking({ ...newTracking, documentId: parseInt(e.target.value) })}
              />
            </Grid>
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
          <Button onClick={() => setCreateDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleCreateTracking} variant="contained">
            Create
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
