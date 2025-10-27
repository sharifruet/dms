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
  Switch,
  FormControlLabel,
  Alert,
  CircularProgress,
  Tooltip,
  Fab
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
  Info as InfoIcon
} from '@mui/icons-material';
import { useAppSelector } from '../hooks/redux';
import integrationService, { IntegrationConfig } from '../services/integrationService';

const Integrations: React.FC = () => {
  const { user } = useAppSelector((state) => state.auth);
  const [integrations, setIntegrations] = useState<IntegrationConfig[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingIntegration, setEditingIntegration] = useState<IntegrationConfig | null>(null);
  const [formData, setFormData] = useState<IntegrationConfig>({
    integrationName: '',
    integrationType: '',
    endpointUrl: '',
    authenticationType: '',
    credentials: '',
    configuration: '',
    status: 'INACTIVE',
    environment: 'PRODUCTION',
    syncFrequencyMinutes: 60,
    retryCount: 0,
    maxRetries: 3,
    timeoutSeconds: 30,
    isEnabled: true
  });

  const integrationTypes = [
    'LDAP',
    'SAP',
    'SALESFORCE',
    'MICROSOFT_GRAPH',
    'SLACK',
    'WEBHOOK',
    'REST_API',
    'SOAP',
    'DATABASE',
    'FILE_SYSTEM'
  ];

  const authenticationTypes = [
    'USERNAME_PASSWORD',
    'OAUTH2',
    'API_KEY',
    'CERTIFICATE',
    'SAML',
    'JWT',
    'BASIC_AUTH',
    'BEARER_TOKEN'
  ];

  const environments = ['PRODUCTION', 'STAGING', 'DEVELOPMENT', 'TEST'];

  const statuses = ['ACTIVE', 'INACTIVE', 'ERROR', 'SYNCING', 'MAINTENANCE'];

  useEffect(() => {
    loadIntegrations();
  }, []);

  const loadIntegrations = async () => {
    try {
      setLoading(true);
      const response = await integrationService.getIntegrations();
      setIntegrations(response.content);
    } catch (err) {
      setError('Failed to load integrations');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateIntegration = async () => {
    try {
      if (editingIntegration) {
        await integrationService.updateIntegration(editingIntegration.id!, formData);
      } else {
        await integrationService.createIntegration(formData);
      }
      setOpenDialog(false);
      setEditingIntegration(null);
      setFormData({
        integrationName: '',
        integrationType: '',
        endpointUrl: '',
        authenticationType: '',
        credentials: '',
        configuration: '',
        status: 'INACTIVE',
        environment: 'PRODUCTION',
        syncFrequencyMinutes: 60,
        retryCount: 0,
        maxRetries: 3,
        timeoutSeconds: 30,
        isEnabled: true
      });
      loadIntegrations();
    } catch (err) {
      setError('Failed to save integration');
    }
  };

  const handleEditIntegration = (integration: IntegrationConfig) => {
    setEditingIntegration(integration);
    setFormData(integration);
    setOpenDialog(true);
  };

  const handleDeleteIntegration = async (id: number) => {
    if (window.confirm('Are you sure you want to delete this integration?')) {
      try {
        await integrationService.deleteIntegration(id);
        loadIntegrations();
      } catch (err) {
        setError('Failed to delete integration');
      }
    }
  };

  const handleTestIntegration = async (id: number) => {
    try {
      const result = await integrationService.testIntegration(id);
      if (result.success) {
        setError(null);
        loadIntegrations();
      } else {
        setError(result.message);
      }
    } catch (err) {
      setError('Failed to test integration');
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'INACTIVE':
        return 'default';
      case 'ERROR':
        return 'error';
      case 'SYNCING':
        return 'warning';
      case 'MAINTENANCE':
        return 'info';
      default:
        return 'default';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return <CheckCircleIcon />;
      case 'ERROR':
        return <ErrorIcon />;
      case 'SYNCING':
        return <RefreshIcon />;
      case 'MAINTENANCE':
        return <WarningIcon />;
      default:
        return <InfoIcon />;
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
          Enterprise Integrations
        </Typography>
        {user?.role === 'ADMIN' && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setOpenDialog(true)}
          >
            Add Integration
          </Button>
        )}
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Grid container spacing={3}>
        {/* Statistics Cards */}
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Total Integrations
              </Typography>
              <Typography variant="h4">
                {integrations.length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Active Integrations
              </Typography>
              <Typography variant="h4" color="success.main">
                {integrations.filter(i => i.status === 'ACTIVE').length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Error Integrations
              </Typography>
              <Typography variant="h4" color="error.main">
                {integrations.filter(i => i.status === 'ERROR').length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Enabled Integrations
              </Typography>
              <Typography variant="h4" color="primary.main">
                {integrations.filter(i => i.isEnabled).length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* Integrations Table */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Integration Configurations
              </Typography>
              <TableContainer component={Paper}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Name</TableCell>
                      <TableCell>Type</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Environment</TableCell>
                      <TableCell>Last Sync</TableCell>
                      <TableCell>Enabled</TableCell>
                      <TableCell>Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {integrations.map((integration) => (
                      <TableRow key={integration.id}>
                        <TableCell>
                          <Typography variant="subtitle2">
                            {integration.integrationName}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={integration.integrationType}
                            size="small"
                            variant="outlined"
                          />
                        </TableCell>
                        <TableCell>
                          <Chip
                            icon={getStatusIcon(integration.status)}
                            label={integration.status}
                            color={getStatusColor(integration.status) as any}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={integration.environment}
                            size="small"
                            variant="outlined"
                          />
                        </TableCell>
                        <TableCell>
                          {integration.lastSyncAt
                            ? new Date(integration.lastSyncAt).toLocaleString()
                            : 'Never'
                          }
                        </TableCell>
                        <TableCell>
                          <Switch
                            checked={integration.isEnabled}
                            disabled={user?.role !== 'ADMIN'}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>
                          <Tooltip title="Test Connection">
                            <IconButton
                              size="small"
                              onClick={() => handleTestIntegration(integration.id!)}
                            >
                              <PlayIcon />
                            </IconButton>
                          </Tooltip>
                          {user?.role === 'ADMIN' && (
                            <>
                              <Tooltip title="Edit">
                                <IconButton
                                  size="small"
                                  onClick={() => handleEditIntegration(integration)}
                                >
                                  <EditIcon />
                                </IconButton>
                              </Tooltip>
                              <Tooltip title="Delete">
                                <IconButton
                                  size="small"
                                  onClick={() => handleDeleteIntegration(integration.id!)}
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
      </Grid>

      {/* Add/Edit Integration Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          {editingIntegration ? 'Edit Integration' : 'Add New Integration'}
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Integration Name"
                value={formData.integrationName}
                onChange={(e) => setFormData({ ...formData, integrationName: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth required>
                <InputLabel>Integration Type</InputLabel>
                <Select
                  value={formData.integrationType}
                  onChange={(e) => setFormData({ ...formData, integrationType: e.target.value })}
                >
                  {integrationTypes.map((type) => (
                    <MenuItem key={type} value={type}>
                      {type}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Endpoint URL"
                value={formData.endpointUrl}
                onChange={(e) => setFormData({ ...formData, endpointUrl: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Authentication Type</InputLabel>
                <Select
                  value={formData.authenticationType}
                  onChange={(e) => setFormData({ ...formData, authenticationType: e.target.value })}
                >
                  {authenticationTypes.map((type) => (
                    <MenuItem key={type} value={type}>
                      {type}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
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
              <FormControl fullWidth>
                <InputLabel>Status</InputLabel>
                <Select
                  value={formData.status}
                  onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                >
                  {statuses.map((status) => (
                    <MenuItem key={status} value={status}>
                      {status}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Sync Frequency (minutes)"
                type="number"
                value={formData.syncFrequencyMinutes}
                onChange={(e) => setFormData({ ...formData, syncFrequencyMinutes: parseInt(e.target.value) })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Configuration (JSON)"
                multiline
                rows={4}
                value={formData.configuration}
                onChange={(e) => setFormData({ ...formData, configuration: e.target.value })}
              />
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
          <Button onClick={handleCreateIntegration} variant="contained">
            {editingIntegration ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Integrations;
