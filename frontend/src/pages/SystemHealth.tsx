import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
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
  Alert,
  CircularProgress,
  Tooltip,
  LinearProgress,
  Tabs,
  Tab,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Divider,
  Switch,
  FormControlLabel,
  Badge
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  PlayArrow as PlayIcon,
  Stop as StopIcon,
  Refresh as RefreshIcon,
  CheckCircle as CheckCircleIcon,
  Error as ErrorIcon,
  Warning as WarningIcon,
  Info as InfoIcon,
  TrendingUp as TrendingUpIcon,
  TrendingDown as TrendingDownIcon,
  Assessment as AssessmentIcon,
  Timeline as TimelineIcon,
  HealthAndSafety as HealthIcon,
  Speed as SpeedIcon,
  Memory as MemoryIcon,
  Storage as StorageIcon,
  NetworkCheck as NetworkIcon
} from '@mui/icons-material';
import { useAppSelector } from '../hooks/redux';
import healthService, { SystemHealthCheck, SystemHealthOverview } from '../services/healthService';

const SystemHealth: React.FC = () => {
  const { user } = useAppSelector((state) => state.auth);
  const [healthChecks, setHealthChecks] = useState<SystemHealthCheck[]>([]);
  const [healthOverview, setHealthOverview] = useState<SystemHealthOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingCheck, setEditingCheck] = useState<SystemHealthCheck | null>(null);
  const [formData, setFormData] = useState<SystemHealthCheck>({
    checkName: '',
    checkType: '',
    status: 'UNKNOWN',
    component: '',
    service: '',
    environment: 'PRODUCTION',
    thresholdValue: 0,
    actualValue: 0,
    responseTimeMs: 0,
    errorMessage: '',
    checkData: '',
    severity: 'LOW',
    retryCount: 0,
    maxRetries: 3,
    checkIntervalSeconds: 300,
    nextCheckAt: '',
    executedAt: '',
    isEnabled: true
  });
  const [activeTab, setActiveTab] = useState(0);
  const [executingChecks, setExecutingChecks] = useState<Set<number>>(new Set());

  const checkTypes = [
    'DATABASE_CONNECTION',
    'REDIS_CONNECTION',
    'ELASTICSEARCH_CONNECTION',
    'FILE_SYSTEM',
    'MEMORY_USAGE',
    'CPU_USAGE',
    'DISK_SPACE',
    'NETWORK_CONNECTIVITY',
    'API_RESPONSE_TIME',
    'SERVICE_AVAILABILITY'
  ];

  const statuses = [
    'HEALTHY',
    'WARNING',
    'CRITICAL',
    'FAILED',
    'UNKNOWN'
  ];

  const severities = [
    'LOW',
    'MEDIUM',
    'HIGH',
    'CRITICAL'
  ];

  const environments = ['PRODUCTION', 'STAGING', 'DEVELOPMENT', 'TEST'];

  const components = ['Database', 'Cache', 'Search', 'Storage', 'System', 'Network', 'API', 'Service'];

  useEffect(() => {
    loadHealthChecks();
    loadHealthOverview();
  }, []);

  const loadHealthChecks = async () => {
    try {
      setLoading(true);
      const response = await healthService.getHealthChecks();
      setHealthChecks(response.content);
    } catch (err) {
      setError('Failed to load health checks');
    } finally {
      setLoading(false);
    }
  };

  const loadHealthOverview = async () => {
    try {
      const overview = await healthService.getSystemHealthOverview();
      setHealthOverview(overview);
    } catch (err) {
      setError('Failed to load health overview');
    }
  };

  const handleCreateHealthCheck = async () => {
    try {
      if (editingCheck) {
        // Update logic would go here
      } else {
        await healthService.createHealthCheck(formData);
      }
      setOpenDialog(false);
      setEditingCheck(null);
      setFormData({
        checkName: '',
        checkType: '',
        status: 'UNKNOWN',
        component: '',
        service: '',
        environment: 'PRODUCTION',
        thresholdValue: 0,
        actualValue: 0,
        responseTimeMs: 0,
        errorMessage: '',
        checkData: '',
        severity: 'LOW',
        retryCount: 0,
        maxRetries: 3,
        checkIntervalSeconds: 300,
        nextCheckAt: '',
        executedAt: '',
        isEnabled: true
      });
      loadHealthChecks();
    } catch (err) {
      setError('Failed to save health check');
    }
  };

  const handleEditCheck = (check: SystemHealthCheck) => {
    setEditingCheck(check);
    setFormData(check);
    setOpenDialog(true);
  };

  const handleDeleteCheck = async (id: number) => {
    if (window.confirm('Are you sure you want to delete this health check?')) {
      try {
        // Delete logic would go here
        loadHealthChecks();
      } catch (err) {
        setError('Failed to delete health check');
      }
    }
  };

  const handleExecuteCheck = async (id: number) => {
    try {
      setExecutingChecks(prev => new Set(prev).add(id));
      const result = await healthService.executeHealthCheck(id);
      if (result.status === 'HEALTHY') {
        setError(null);
      } else {
        setError(`Health check failed: ${result.errorMessage}`);
      }
      loadHealthChecks();
    } catch (err) {
      setError('Failed to execute health check');
    } finally {
      setExecutingChecks(prev => {
        const newSet = new Set(prev);
        newSet.delete(id);
        return newSet;
      });
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'HEALTHY':
        return 'success';
      case 'WARNING':
        return 'warning';
      case 'CRITICAL':
        return 'error';
      case 'FAILED':
        return 'error';
      default:
        return 'default';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'HEALTHY':
        return <CheckCircleIcon />;
      case 'WARNING':
        return <WarningIcon />;
      case 'CRITICAL':
        return <ErrorIcon />;
      case 'FAILED':
        return <ErrorIcon />;
      default:
        return <InfoIcon />;
    }
  };

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'LOW':
        return 'success';
      case 'MEDIUM':
        return 'warning';
      case 'HIGH':
        return 'error';
      case 'CRITICAL':
        return 'error';
      default:
        return 'default';
    }
  };

  const getCheckTypeIcon = (checkType: string) => {
    switch (checkType) {
      case 'DATABASE_CONNECTION':
        return <StorageIcon />;
      case 'MEMORY_USAGE':
        return <MemoryIcon />;
      case 'CPU_USAGE':
        return <SpeedIcon />;
      case 'NETWORK_CONNECTIVITY':
        return <NetworkIcon />;
      default:
        return <HealthIcon />;
    }
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
          System Health Monitoring
        </Typography>
        {user?.role === 'ADMIN' && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setOpenDialog(true)}
          >
            Add Health Check
          </Button>
        )}
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Grid container spacing={3}>
        {/* Health Overview Cards */}
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Overall Health Score
              </Typography>
              <Typography variant="h4" color="primary.main">
                {healthOverview?.overallHealthScore?.toFixed(1) || '0.0'}%
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Healthy Checks
              </Typography>
              <Typography variant="h4" color="success.main">
                {healthChecks.filter(c => c.status === 'HEALTHY').length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Warning Checks
              </Typography>
              <Typography variant="h4" color="warning.main">
                {healthChecks.filter(c => c.status === 'WARNING').length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Critical Issues
              </Typography>
              <Typography variant="h4" color="error.main">
                {healthOverview?.criticalIssues || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* Health Checks Table */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Health Checks
              </Typography>
              <TableContainer component={Paper}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Check Name</TableCell>
                      <TableCell>Type</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Component</TableCell>
                      <TableCell>Severity</TableCell>
                      <TableCell>Response Time</TableCell>
                      <TableCell>Last Check</TableCell>
                      <TableCell>Enabled</TableCell>
                      <TableCell>Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {healthChecks.map((check) => (
                      <TableRow key={check.id}>
                        <TableCell>
                          <Box display="flex" alignItems="center">
                            {getCheckTypeIcon(check.checkType)}
                            <Box sx={{ ml: 1 }}>
                              <Typography variant="subtitle2">
                                {check.checkName}
                              </Typography>
                              <Typography variant="caption" color="textSecondary">
                                {check.service}
                              </Typography>
                            </Box>
                          </Box>
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={check.checkType}
                            size="small"
                            variant="outlined"
                          />
                        </TableCell>
                        <TableCell>
                          <Chip
                            icon={getStatusIcon(check.status)}
                            label={check.status}
                            color={getStatusColor(check.status) as any}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {check.component}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={check.severity}
                            color={getSeverityColor(check.severity) as any}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {check.responseTimeMs ? `${check.responseTimeMs}ms` : 'N/A'}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {check.executedAt
                              ? new Date(check.executedAt).toLocaleString()
                              : 'Never'
                            }
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Switch
                            checked={check.isEnabled}
                            disabled={user?.role !== 'ADMIN'}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>
                          <Tooltip title="Execute Check">
                            <IconButton
                              size="small"
                              onClick={() => handleExecuteCheck(check.id!)}
                              disabled={executingChecks.has(check.id!)}
                            >
                              {executingChecks.has(check.id!) ? (
                                <CircularProgress size={16} />
                              ) : (
                                <PlayIcon />
                              )}
                            </IconButton>
                          </Tooltip>
                          {user?.role === 'ADMIN' && (
                            <>
                              <Tooltip title="Edit">
                                <IconButton
                                  size="small"
                                  onClick={() => handleEditCheck(check)}
                                >
                                  <EditIcon />
                                </IconButton>
                              </Tooltip>
                              <Tooltip title="Delete">
                                <IconButton
                                  size="small"
                                  onClick={() => handleDeleteCheck(check.id!)}
                                  color="error"
                                >
                                  <DeleteIcon />
                                </IconButton>
                              </Tooltip>
                            </>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Grid>

        {/* Component Health Overview */}
        {healthOverview?.componentHealth && (
          <Grid item xs={12}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Component Health Overview
                </Typography>
                <Grid container spacing={2}>
                  {Object.entries(healthOverview.componentHealth).map(([component, health]: [string, any]) => (
                    <Grid item xs={12} sm={6} md={3} key={component}>
                      <Card variant="outlined">
                        <CardContent>
                          <Typography variant="subtitle2" gutterBottom>
                            {component}
                          </Typography>
                          <Typography variant="h6" color="primary.main">
                            {health.healthScore?.toFixed(1)}%
                          </Typography>
                          <Typography variant="caption" color="textSecondary">
                            {health.healthyChecks}/{health.totalChecks} healthy
                          </Typography>
                        </CardContent>
                      </Card>
                    </Grid>
                  ))}
                </Grid>
              </CardContent>
            </Card>
          </Grid>
        )}
      </Grid>

      {/* Add/Edit Health Check Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          {editingCheck ? 'Edit Health Check' : 'Add New Health Check'}
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Check Name"
                value={formData.checkName}
                onChange={(e) => setFormData({ ...formData, checkName: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth required>
                <InputLabel>Check Type</InputLabel>
                <Select
                  value={formData.checkType}
                  onChange={(e) => setFormData({ ...formData, checkType: e.target.value })}
                >
                  {checkTypes.map((type) => (
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
                label="Component"
                value={formData.component}
                onChange={(e) => setFormData({ ...formData, component: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Service"
                value={formData.service}
                onChange={(e) => setFormData({ ...formData, service: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Environment</InputLabel>
                <Select
                  value={formData.environment}
                  onChange={(e) => setFormData({ ...formData, environment: e.target.value })}
                >
                  {environments.map((env) => (
                    <MenuItem key={env} value={env}>
                      {env}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Threshold Value"
                type="number"
                value={formData.thresholdValue}
                onChange={(e) => setFormData({ ...formData, thresholdValue: parseFloat(e.target.value) })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Check Interval (seconds)"
                type="number"
                value={formData.checkIntervalSeconds}
                onChange={(e) => setFormData({ ...formData, checkIntervalSeconds: parseInt(e.target.value) })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Severity</InputLabel>
                <Select
                  value={formData.severity}
                  onChange={(e) => setFormData({ ...formData, severity: e.target.value })}
                >
                  {severities.map((severity) => (
                    <MenuItem key={severity} value={severity}>
                      {severity}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.isEnabled}
                    onChange={(e) => setFormData({ ...formData, isEnabled: e.target.checked })}
                  />
                }
                label="Enabled"
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button onClick={handleCreateHealthCheck} variant="contained">
            {editingCheck ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SystemHealth;
