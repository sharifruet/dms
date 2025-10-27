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
  FormControlLabel
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
  Psychology as PsychologyIcon,
  Speed as SpeedIcon
} from '@mui/icons-material';
import { useAppSelector } from '../hooks/redux';
import mlService, { MLModel, MLModelPerformance } from '../services/mlService';

const MachineLearning: React.FC = () => {
  const { user } = useAppSelector((state) => state.auth);
  const [mlModels, setMlModels] = useState<MLModel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingModel, setEditingModel] = useState<MLModel | null>(null);
  const [formData, setFormData] = useState<MLModel>({
    modelName: '',
    modelType: '',
    modelDescription: '',
    modelVersion: '1.0.0',
    trainingData: '',
    modelParameters: '',
    performanceMetrics: '',
    status: 'TRAINING',
    accuracy: 0,
    precisionScore: 0,
    recallScore: 0,
    f1Score: 0,
    deploymentStatus: 'NOT_DEPLOYED',
    modelSizeMb: 0,
    isActive: false
  });
  const [activeTab, setActiveTab] = useState(0);
  const [trainingProgress, setTrainingProgress] = useState<{ [key: number]: number }>({});

  const modelTypes = [
    'CLASSIFICATION',
    'REGRESSION',
    'CLUSTERING',
    'ANOMALY_DETECTION',
    'RECOMMENDATION',
    'NATURAL_LANGUAGE_PROCESSING',
    'COMPUTER_VISION',
    'TIME_SERIES',
    'DEEP_LEARNING',
    'ENSEMBLE'
  ];

  const modelStatuses = [
    'TRAINING',
    'TRAINED',
    'DEPLOYED',
    'FAILED',
    'ARCHIVED'
  ];

  const deploymentStatuses = [
    'NOT_DEPLOYED',
    'DEPLOYING',
    'DEPLOYED',
    'FAILED',
    'ROLLING_BACK'
  ];

  useEffect(() => {
    loadMLModels();
  }, []);

  const loadMLModels = async () => {
    try {
      setLoading(true);
      const response = await mlService.getMLModels();
      setMlModels(response.content);
    } catch (err) {
      setError('Failed to load ML models');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateMLModel = async () => {
    try {
      if (editingModel) {
        await mlService.updateMLModel(editingModel.id!, formData);
      } else {
        await mlService.createMLModel(formData);
      }
      setOpenDialog(false);
      setEditingModel(null);
      setFormData({
        modelName: '',
        modelType: '',
        modelDescription: '',
        modelVersion: '1.0.0',
        trainingData: '',
        modelParameters: '',
        performanceMetrics: '',
        status: 'TRAINING',
        accuracy: 0,
        precisionScore: 0,
        recallScore: 0,
        f1Score: 0,
        deploymentStatus: 'NOT_DEPLOYED',
        modelSizeMb: 0,
        isActive: false
      });
      loadMLModels();
    } catch (err) {
      setError('Failed to save ML model');
    }
  };

  const handleEditModel = (model: MLModel) => {
    setEditingModel(model);
    setFormData(model);
    setOpenDialog(true);
  };

  const handleDeleteModel = async (id: number) => {
    if (window.confirm('Are you sure you want to delete this ML model?')) {
      try {
        await mlService.deleteMLModel(id);
        loadMLModels();
      } catch (err) {
        setError('Failed to delete ML model');
      }
    }
  };

  const handleTrainModel = async (id: number) => {
    try {
      const result = await mlService.trainMLModel(id);
      if (result.success) {
        setError(null);
        // Simulate training progress
        setTrainingProgress(prev => ({ ...prev, [id]: 0 }));
        simulateTrainingProgress(id);
        loadMLModels();
      } else {
        setError(result.message);
      }
    } catch (err) {
      setError('Failed to train model');
    }
  };

  const handleDeployModel = async (id: number) => {
    try {
      const result = await mlService.deployMLModel(id);
      if (result.success) {
        setError(null);
        loadMLModels();
      } else {
        setError(result.message);
      }
    } catch (err) {
      setError('Failed to deploy model');
    }
  };

  const simulateTrainingProgress = (id: number) => {
    const interval = setInterval(() => {
      setTrainingProgress(prev => {
        const current = prev[id] || 0;
        if (current >= 100) {
          clearInterval(interval);
          return { ...prev, [id]: 100 };
        }
        return { ...prev, [id]: current + 10 };
      });
    }, 1000);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'TRAINED':
        return 'success';
      case 'TRAINING':
        return 'warning';
      case 'DEPLOYED':
        return 'info';
      case 'FAILED':
        return 'error';
      default:
        return 'default';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'TRAINED':
        return <CheckCircleIcon />;
      case 'TRAINING':
        return <RefreshIcon />;
      case 'DEPLOYED':
        return <PlayIcon />;
      case 'FAILED':
        return <ErrorIcon />;
      default:
        return <InfoIcon />;
    }
  };

  const getDeploymentStatusColor = (status: string) => {
    switch (status) {
      case 'DEPLOYED':
        return 'success';
      case 'DEPLOYING':
        return 'warning';
      case 'NOT_DEPLOYED':
        return 'default';
      case 'FAILED':
        return 'error';
      default:
        return 'default';
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
          Machine Learning Models
        </Typography>
        {user?.role === 'ADMIN' && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setOpenDialog(true)}
          >
            Add ML Model
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
                Total Models
              </Typography>
              <Typography variant="h4">
                {mlModels.length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Active Models
              </Typography>
              <Typography variant="h4" color="success.main">
                {mlModels.filter(m => m.isActive).length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Deployed Models
              </Typography>
              <Typography variant="h4" color="primary.main">
                {mlModels.filter(m => m.deploymentStatus === 'DEPLOYED').length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Average Accuracy
              </Typography>
              <Typography variant="h4" color="info.main">
                {mlModels.length > 0
                  ? (mlModels.reduce((sum, m) => sum + (m.accuracy || 0), 0) / mlModels.length).toFixed(2)
                  : '0.00'
                }
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* ML Models Table */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                ML Models
              </Typography>
              <TableContainer component={Paper}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Model Name</TableCell>
                      <TableCell>Type</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Deployment</TableCell>
                      <TableCell>Accuracy</TableCell>
                      <TableCell>Version</TableCell>
                      <TableCell>Active</TableCell>
                      <TableCell>Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {mlModels.map((model) => (
                      <TableRow key={model.id}>
                        <TableCell>
                          <Typography variant="subtitle2">
                            {model.modelName}
                          </Typography>
                          <Typography variant="caption" color="textSecondary">
                            {model.modelDescription}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={model.modelType}
                            size="small"
                            variant="outlined"
                          />
                        </TableCell>
                        <TableCell>
                          <Chip
                            icon={getStatusIcon(model.status)}
                            label={model.status}
                            color={getStatusColor(model.status) as any}
                            size="small"
                          />
                          {model.status === 'TRAINING' && trainingProgress[model.id!] !== undefined && (
                            <Box sx={{ mt: 1 }}>
                              <LinearProgress
                                variant="determinate"
                                value={trainingProgress[model.id!]}
                                sx={{ height: 4 }}
                              />
                              <Typography variant="caption" color="textSecondary">
                                {trainingProgress[model.id!]}%
                              </Typography>
                            </Box>
                          )}
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={model.deploymentStatus}
                            color={getDeploymentStatusColor(model.deploymentStatus) as any}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {model.accuracy ? `${(model.accuracy * 100).toFixed(1)}%` : 'N/A'}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {model.modelVersion}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Switch
                            checked={model.isActive}
                            disabled={user?.role !== 'ADMIN'}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>
                          <Tooltip title="Train Model">
                            <IconButton
                              size="small"
                              onClick={() => handleTrainModel(model.id!)}
                              disabled={model.status === 'TRAINING'}
                            >
                              <PlayIcon />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Deploy Model">
                            <IconButton
                              size="small"
                              onClick={() => handleDeployModel(model.id!)}
                              disabled={model.status !== 'TRAINED' || model.deploymentStatus === 'DEPLOYED'}
                            >
                              <SpeedIcon />
                            </IconButton>
                          </Tooltip>
                          {user?.role === 'ADMIN' && (
                            <>
                              <Tooltip title="Edit">
                                <IconButton
                                  size="small"
                                  onClick={() => handleEditModel(model)}
                                >
                                  <EditIcon />
                                </IconButton>
                              </Tooltip>
                              <Tooltip title="Delete">
                                <IconButton
                                  size="small"
                                  onClick={() => handleDeleteModel(model.id!)}
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

      {/* Add/Edit ML Model Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          {editingModel ? 'Edit ML Model' : 'Add New ML Model'}
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Model Name"
                value={formData.modelName}
                onChange={(e) => setFormData({ ...formData, modelName: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth required>
                <InputLabel>Model Type</InputLabel>
                <Select
                  value={formData.modelType}
                  onChange={(e) => setFormData({ ...formData, modelType: e.target.value })}
                >
                  {modelTypes.map((type) => (
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
                label="Model Description"
                multiline
                rows={2}
                value={formData.modelDescription}
                onChange={(e) => setFormData({ ...formData, modelDescription: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Model Version"
                value={formData.modelVersion}
                onChange={(e) => setFormData({ ...formData, modelVersion: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Status</InputLabel>
                <Select
                  value={formData.status}
                  onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                >
                  {modelStatuses.map((status) => (
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
                label="Accuracy"
                type="number"
                inputProps={{ min: 0, max: 1, step: 0.01 }}
                value={formData.accuracy}
                onChange={(e) => setFormData({ ...formData, accuracy: parseFloat(e.target.value) })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Model Size (MB)"
                type="number"
                value={formData.modelSizeMb}
                onChange={(e) => setFormData({ ...formData, modelSizeMb: parseFloat(e.target.value) })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Training Data (JSON)"
                multiline
                rows={3}
                value={formData.trainingData}
                onChange={(e) => setFormData({ ...formData, trainingData: e.target.value })}
                placeholder='{"features": ["feature1", "feature2"], "target": "target"}'
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Model Parameters (JSON)"
                multiline
                rows={3}
                value={formData.modelParameters}
                onChange={(e) => setFormData({ ...formData, modelParameters: e.target.value })}
                placeholder='{"learning_rate": 0.01, "epochs": 100}'
              />
            </Grid>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.isActive}
                    onChange={(e) => setFormData({ ...formData, isActive: e.target.checked })}
                  />
                }
                label="Active"
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button onClick={handleCreateMLModel} variant="contained">
            {editingModel ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default MachineLearning;
