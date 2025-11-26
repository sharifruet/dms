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
  Badge,
  Tooltip
} from '@mui/material';
import {
  PlayArrow,
  CheckCircle,
  Cancel,
  Visibility,
  Add,
  Refresh,
  Assignment,
  Timeline,
  Warning,
  Edit as EditIcon,
  Link as LinkIcon,
  LinkOff as LinkOffIcon,
  AccountBalance as BudgetIcon,
} from '@mui/icons-material';
import { workflowService, WorkflowInstance, WorkflowStep, AppEntry } from '../services/workflowService';
import AppEntrySelector from '../components/AppEntrySelector';

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
      id={`workflow-tabpanel-${index}`}
      aria-labelledby={`workflow-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

const Workflows: React.FC = () => {
  const [activeTab, setActiveTab] = useState(0);
  const [instances, setInstances] = useState<WorkflowInstance[]>([]);
  const [steps, setSteps] = useState<WorkflowStep[]>([]);
  const [overdueInstances, setOverdueInstances] = useState<WorkflowInstance[]>([]);
  const [statistics, setStatistics] = useState<any>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedStep, setSelectedStep] = useState<WorkflowStep | null>(null);
  const [actionDialogOpen, setActionDialogOpen] = useState(false);
  const [actionType, setActionType] = useState<'complete' | 'reject'>('complete');
  const [actionTaken, setActionTaken] = useState('');
  const [comments, setComments] = useState('');
  
  // Workflow details dialog
  const [detailsDialogOpen, setDetailsDialogOpen] = useState(false);
  const [selectedInstance, setSelectedInstance] = useState<WorkflowInstance | null>(null);
  const [linkedAppEntry, setLinkedAppEntry] = useState<AppEntry | null>(null);
  const [loadingAppEntry, setLoadingAppEntry] = useState(false);
  
  // Change APP Entry dialog
  const [changeAppEntryDialogOpen, setChangeAppEntryDialogOpen] = useState(false);
  const [selectedAppEntryId, setSelectedAppEntryId] = useState<number | null>(null);
  const [changingAppEntry, setChangingAppEntry] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [instancesData, stepsData, overdueData, statsData] = await Promise.all([
        workflowService.getWorkflowInstances(0, 100),
        workflowService.getWorkflowSteps(0, 100),
        workflowService.getOverdueWorkflowInstances(),
        workflowService.getWorkflowStatistics()
      ]);

      setInstances(instancesData.content);
      setSteps(stepsData.content);
      setOverdueInstances(overdueData);
      setStatistics(statsData);
    } catch (err) {
      setError('Failed to load workflow data');
    } finally {
      setLoading(false);
    }
  };

  const handleStepAction = (step: WorkflowStep, type: 'complete' | 'reject') => {
    setSelectedStep(step);
    setActionType(type);
    setActionDialogOpen(true);
    setActionTaken('');
    setComments('');
  };

  const submitStepAction = async () => {
    if (!selectedStep) return;

    try {
      if (actionType === 'complete') {
        await workflowService.completeWorkflowStep(selectedStep.id, {
          actionTaken,
          comments
        });
      } else {
        await workflowService.rejectWorkflowStep(selectedStep.id, {
          reason: comments
        });
      }

      setActionDialogOpen(false);
      loadData();
    } catch (err) {
      setError('Failed to update workflow step');
    }
  };

  // Handle view workflow details
  const handleViewDetails = async (instance: WorkflowInstance) => {
    setSelectedInstance(instance);
    setDetailsDialogOpen(true);
    setLoadingAppEntry(true);
    
    // Load APP entry for this workflow
    try {
      const appEntryResponse = await workflowService.getAppEntryForWorkflow(instance.workflow.id);
      if (appEntryResponse.success && appEntryResponse.appEntry) {
        setLinkedAppEntry(appEntryResponse.appEntry);
      } else {
        setLinkedAppEntry(null);
      }
    } catch (err: any) {
      console.error('Failed to load APP entry:', err);
      setLinkedAppEntry(null);
    } finally {
      setLoadingAppEntry(false);
    }
  };

  // Handle change APP entry
  const handleOpenChangeAppEntry = () => {
    setSelectedAppEntryId(linkedAppEntry?.id || null);
    setChangeAppEntryDialogOpen(true);
  };

  const handleSaveAppEntry = async () => {
    if (!selectedInstance) return;
    
    setChangingAppEntry(true);
    try {
      const workflowId = selectedInstance.workflow.id;
      
      if (selectedAppEntryId) {
        // Link or change APP entry
        const response = await workflowService.linkWorkflowToAppEntry(workflowId, selectedAppEntryId);
        if (response.success && response.workflow) {
          setLinkedAppEntry(response.workflow.appEntry || null);
          setChangeAppEntryDialogOpen(false);
          setError(null);
        } else {
          setError(response.message || 'Failed to link APP entry');
        }
      } else {
        // Unlink APP entry
        const response = await workflowService.unlinkWorkflowFromAppEntry(workflowId);
        if (response.success) {
          setLinkedAppEntry(null);
          setChangeAppEntryDialogOpen(false);
          setError(null);
        } else {
          setError(response.message || 'Failed to unlink APP entry');
        }
      }
      
      // Reload data to refresh workflow instances
      loadData();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to update APP entry');
    } finally {
      setChangingAppEntry(false);
    }
  };

  const formatFiscalYear = (year: number): string => {
    const nextYear = (year + 1) % 100;
    return `${year}-${nextYear.toString().padStart(2, '0')}`;
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED': return 'success';
      case 'IN_PROGRESS': return 'info';
      case 'PENDING': return 'warning';
      case 'REJECTED': return 'error';
      case 'CANCELLED': return 'default';
      default: return 'default';
    }
  };

  const getPriorityColor = (priority: number) => {
    if (priority >= 3) return 'error';
    if (priority >= 2) return 'warning';
    return 'success';
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" component="h1">
          Workflow Management
        </Typography>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={() => {/* TODO: Implement create workflow */}}
        >
          Create Workflow
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Statistics Cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Total Workflows
              </Typography>
              <Typography variant="h4">
                {statistics.totalWorkflows || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Active Instances
              </Typography>
              <Typography variant="h4">
                {(statistics.pendingInstances || 0) + (statistics.inProgressInstances || 0)}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Completed
              </Typography>
              <Typography variant="h4">
                {statistics.completedInstances || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Overdue
              </Typography>
              <Typography variant="h4" color="error">
                {overdueInstances.length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs value={activeTab} onChange={(e, newValue) => setActiveTab(newValue)}>
          <Tab label="My Workflows" />
          <Tab 
            label={
              <Badge badgeContent={steps.length} color="primary">
                Assigned Steps
              </Badge>
            } 
          />
          <Tab 
            label={
              <Badge badgeContent={overdueInstances.length} color="error">
                Overdue
              </Badge>
            } 
          />
        </Tabs>
      </Box>

      {/* My Workflows Tab */}
      <TabPanel value={activeTab} index={0}>
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Workflow</TableCell>
                <TableCell>Document</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Progress</TableCell>
                <TableCell>Budget Entry</TableCell>
                <TableCell>Due Date</TableCell>
                <TableCell>Priority</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {instances.map((instance) => (
                <TableRow key={instance.id}>
                  <TableCell>
                    <Typography variant="subtitle2">
                      {instance.workflow.name}
                    </Typography>
                    <Typography variant="caption" color="textSecondary">
                      {instance.workflow.type}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    {instance.document?.originalName || 'N/A'}
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={instance.status}
                      color={getStatusColor(instance.status) as any}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    <Box display="flex" alignItems="center">
                      <Typography variant="body2" sx={{ mr: 1 }}>
                        {instance.currentStep}/{instance.totalSteps}
                      </Typography>
                      <Box
                        sx={{
                          width: 60,
                          height: 8,
                          backgroundColor: 'grey.300',
                          borderRadius: 1,
                          overflow: 'hidden'
                        }}
                      >
                        <Box
                          sx={{
                            width: `${(instance.currentStep / instance.totalSteps) * 100}%`,
                            height: '100%',
                            backgroundColor: 'primary.main',
                            transition: 'width 0.3s ease'
                          }}
                        />
                      </Box>
                    </Box>
                  </TableCell>
                  <TableCell>
                    {instance.workflow.appEntry ? (
                      <Tooltip 
                        title={`FY ${formatFiscalYear(instance.workflow.appEntry.fiscalYear)}, Installment ${instance.workflow.appEntry.releaseInstallmentNo || 'N/A'}`}
                      >
                        <Chip
                          icon={<BudgetIcon />}
                          label={`FY ${formatFiscalYear(instance.workflow.appEntry.fiscalYear)}`}
                          size="small"
                          color="primary"
                          variant="outlined"
                        />
                      </Tooltip>
                    ) : (
                      <Typography variant="caption" color="text.secondary">
                        Not linked
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell>
                    {instance.dueDate ? new Date(instance.dueDate).toLocaleDateString() : 'N/A'}
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={`P${instance.priority}`}
                      color={getPriorityColor(instance.priority) as any}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    <Tooltip title="View Details">
                      <IconButton size="small" onClick={() => handleViewDetails(instance)}>
                        <Visibility />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </TabPanel>

      {/* Assigned Steps Tab */}
      <TabPanel value={activeTab} index={1}>
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Step</TableCell>
                <TableCell>Workflow</TableCell>
                <TableCell>Document</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Due Date</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {steps.map((step) => (
                <TableRow key={step.id}>
                  <TableCell>
                    <Typography variant="subtitle2">
                      {step.stepName}
                    </Typography>
                    <Typography variant="caption" color="textSecondary">
                      {step.stepDescription}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    {step.workflowInstance.workflow.name}
                  </TableCell>
                  <TableCell>
                    {step.workflowInstance.document?.originalName || 'N/A'}
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={step.status}
                      color={getStatusColor(step.status) as any}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    {step.dueDate ? new Date(step.dueDate).toLocaleDateString() : 'N/A'}
                  </TableCell>
                  <TableCell>
                    <Tooltip title="Complete Step">
                      <IconButton
                        size="small"
                        color="success"
                        onClick={() => handleStepAction(step, 'complete')}
                      >
                        <CheckCircle />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Reject Step">
                      <IconButton
                        size="small"
                        color="error"
                        onClick={() => handleStepAction(step, 'reject')}
                      >
                        <Cancel />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </TabPanel>

      {/* Overdue Tab */}
      <TabPanel value={activeTab} index={2}>
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Workflow</TableCell>
                <TableCell>Document</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Budget Entry</TableCell>
                <TableCell>Due Date</TableCell>
                <TableCell>Days Overdue</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {overdueInstances.map((instance) => (
                <TableRow key={instance.id}>
                  <TableCell>
                    <Typography variant="subtitle2">
                      {instance.workflow.name}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    {instance.document?.originalName || 'N/A'}
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={instance.status}
                      color={getStatusColor(instance.status) as any}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    {instance.workflow.appEntry ? (
                      <Tooltip 
                        title={`FY ${formatFiscalYear(instance.workflow.appEntry.fiscalYear)}, Installment ${instance.workflow.appEntry.releaseInstallmentNo || 'N/A'}`}
                      >
                        <Chip
                          icon={<BudgetIcon />}
                          label={`FY ${formatFiscalYear(instance.workflow.appEntry.fiscalYear)}`}
                          size="small"
                          color="primary"
                          variant="outlined"
                        />
                      </Tooltip>
                    ) : (
                      <Typography variant="caption" color="text.secondary">
                        Not linked
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell>
                    {instance.dueDate ? new Date(instance.dueDate).toLocaleDateString() : 'N/A'}
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={`${Math.ceil((new Date().getTime() - new Date(instance.dueDate).getTime()) / (1000 * 60 * 60 * 24))} days`}
                      color="error"
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    <Tooltip title="View Details">
                      <IconButton size="small" onClick={() => handleViewDetails(instance)}>
                        <Visibility />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </TabPanel>

      {/* Action Dialog */}
      <Dialog open={actionDialogOpen} onClose={() => setActionDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          {actionType === 'complete' ? 'Complete Workflow Step' : 'Reject Workflow Step'}
        </DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Action Taken"
            fullWidth
            variant="outlined"
            value={actionTaken}
            onChange={(e) => setActionTaken(e.target.value)}
            sx={{ mb: 2 }}
          />
          <TextField
            margin="dense"
            label="Comments"
            fullWidth
            multiline
            rows={4}
            variant="outlined"
            value={comments}
            onChange={(e) => setComments(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setActionDialogOpen(false)}>Cancel</Button>
          <Button
            onClick={submitStepAction}
            variant="contained"
            color={actionType === 'complete' ? 'success' : 'error'}
          >
            {actionType === 'complete' ? 'Complete' : 'Reject'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Workflow Details Dialog */}
      <Dialog 
        open={detailsDialogOpen} 
        onClose={() => {
          setDetailsDialogOpen(false);
          setSelectedInstance(null);
          setLinkedAppEntry(null);
        }} 
        maxWidth="md" 
        fullWidth
      >
        <DialogTitle>
          Workflow Details
        </DialogTitle>
        <DialogContent>
          {selectedInstance && (
            <Box sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 3 }}>
              {/* Workflow Information */}
              <Box>
                <Typography variant="h6" gutterBottom>Workflow Information</Typography>
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="body2" color="text.secondary">Name</Typography>
                    <Typography variant="body1">{selectedInstance.workflow.name}</Typography>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="body2" color="text.secondary">Type</Typography>
                    <Typography variant="body1">{selectedInstance.workflow.type}</Typography>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="body2" color="text.secondary">Status</Typography>
                    <Chip
                      label={selectedInstance.status}
                      color={getStatusColor(selectedInstance.status) as any}
                      size="small"
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="body2" color="text.secondary">Progress</Typography>
                    <Typography variant="body1">
                      {selectedInstance.currentStep} / {selectedInstance.totalSteps}
                    </Typography>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="body2" color="text.secondary">Document</Typography>
                    <Typography variant="body1">
                      {selectedInstance.document?.originalName || 'N/A'}
                    </Typography>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="body2" color="text.secondary">Initiated By</Typography>
                    <Typography variant="body1">
                      {selectedInstance.initiatedBy?.username || 'N/A'}
                    </Typography>
                  </Grid>
                  {selectedInstance.dueDate && (
                    <Grid item xs={12} sm={6}>
                      <Typography variant="body2" color="text.secondary">Due Date</Typography>
                      <Typography variant="body1">
                        {new Date(selectedInstance.dueDate).toLocaleDateString()}
                      </Typography>
                    </Grid>
                  )}
                </Grid>
              </Box>

              {/* Linked APP Entry Section */}
              <Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Typography variant="h6">Linked Budget Entry (APP Entry)</Typography>
                  <Button
                    variant="outlined"
                    size="small"
                    startIcon={<EditIcon />}
                    onClick={handleOpenChangeAppEntry}
                  >
                    Change APP Entry
                  </Button>
                </Box>
                
                {loadingAppEntry ? (
                  <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
                    <CircularProgress size={24} />
                  </Box>
                ) : linkedAppEntry ? (
                  <Card variant="outlined">
                    <CardContent>
                      <Grid container spacing={2}>
                        <Grid item xs={12} sm={6}>
                          <Typography variant="body2" color="text.secondary">
                            Fiscal Year
                          </Typography>
                          <Typography variant="body1" fontWeight="medium">
                            {formatFiscalYear(linkedAppEntry.fiscalYear)}
                          </Typography>
                        </Grid>
                        <Grid item xs={12} sm={6}>
                          <Typography variant="body2" color="text.secondary">
                            Installment No.
                          </Typography>
                          <Typography variant="body1" fontWeight="medium">
                            {linkedAppEntry.releaseInstallmentNo || 'N/A'}
                          </Typography>
                        </Grid>
                        {linkedAppEntry.allocationType && (
                          <Grid item xs={12} sm={6}>
                            <Typography variant="body2" color="text.secondary">
                              Allocation Type
                            </Typography>
                            <Typography variant="body1" fontWeight="medium">
                              {linkedAppEntry.allocationType}
                            </Typography>
                          </Grid>
                        )}
                        {linkedAppEntry.allocationAmount && (
                          <Grid item xs={12} sm={6}>
                            <Typography variant="body2" color="text.secondary">
                              Allocation Amount
                            </Typography>
                            <Typography variant="body1" fontWeight="medium">
                              {new Intl.NumberFormat('en-BD', {
                                style: 'currency',
                                currency: 'BDT',
                                minimumFractionDigits: 0,
                                maximumFractionDigits: 0,
                              }).format(linkedAppEntry.allocationAmount)}
                            </Typography>
                          </Grid>
                        )}
                        {linkedAppEntry.budgetReleaseDate && (
                          <Grid item xs={12} sm={6}>
                            <Typography variant="body2" color="text.secondary">
                              Budget Release Date
                            </Typography>
                            <Typography variant="body1" fontWeight="medium">
                              {new Date(linkedAppEntry.budgetReleaseDate).toLocaleDateString()}
                            </Typography>
                          </Grid>
                        )}
                        {linkedAppEntry.referenceMemoNumber && (
                          <Grid item xs={12} sm={6}>
                            <Typography variant="body2" color="text.secondary">
                              Reference/Memo Number
                            </Typography>
                            <Typography variant="body1" fontWeight="medium">
                              {linkedAppEntry.referenceMemoNumber}
                            </Typography>
                          </Grid>
                        )}
                      </Grid>
                    </CardContent>
                  </Card>
                ) : (
                  <Alert severity="info">
                    No APP entry (budget entry) is currently linked to this workflow.
                  </Alert>
                )}
              </Box>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => {
            setDetailsDialogOpen(false);
            setSelectedInstance(null);
            setLinkedAppEntry(null);
          }}>
            Close
          </Button>
        </DialogActions>
      </Dialog>

      {/* Change APP Entry Dialog */}
      <Dialog 
        open={changeAppEntryDialogOpen} 
        onClose={() => {
          setChangeAppEntryDialogOpen(false);
          setSelectedAppEntryId(null);
        }} 
        maxWidth="sm" 
        fullWidth
      >
        <DialogTitle>
          Change APP Entry (Budget Entry)
        </DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
            <AppEntrySelector
              value={selectedAppEntryId}
              onChange={(appEntryId) => setSelectedAppEntryId(appEntryId)}
              required={false}
              disabled={changingAppEntry}
              showDetails={true}
            />
            <Alert severity="info">
              Select an APP entry to link to this workflow. Leave blank to unlink the current APP entry.
            </Alert>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button 
            onClick={() => {
              setChangeAppEntryDialogOpen(false);
              setSelectedAppEntryId(null);
            }}
            disabled={changingAppEntry}
          >
            Cancel
          </Button>
          <Button
            onClick={handleSaveAppEntry}
            variant="contained"
            disabled={changingAppEntry}
          >
            {changingAppEntry ? (
              <>
                <CircularProgress size={20} sx={{ mr: 1 }} />
                Saving...
              </>
            ) : (
              selectedAppEntryId ? 'Link APP Entry' : 'Unlink APP Entry'
            )}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Workflows;
