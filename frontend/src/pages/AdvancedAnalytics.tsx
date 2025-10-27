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
  Divider
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
  Timeline as TimelineIcon
} from '@mui/icons-material';
import { useAppSelector } from '../hooks/redux';
import analyticsService, { AnalyticsData, AnalyticsInsights } from '../services/analyticsService';

const AdvancedAnalytics: React.FC = () => {
  const { user } = useAppSelector((state) => state.auth);
  const [analyticsData, setAnalyticsData] = useState<AnalyticsData[]>([]);
  const [insights, setInsights] = useState<AnalyticsInsights | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingData, setEditingData] = useState<AnalyticsData | null>(null);
  const [formData, setFormData] = useState<AnalyticsData>({
    analyticsType: '',
    metricName: '',
    metricValue: 0,
    dimensions: '',
    tags: '',
    userId: '',
    sessionId: '',
    ipAddress: '',
    userAgent: ''
  });
  const [activeTab, setActiveTab] = useState(0);

  const analyticsTypes = [
    'USER_BEHAVIOR',
    'SYSTEM_PERFORMANCE',
    'DOCUMENT_ANALYTICS',
    'SEARCH_ANALYTICS',
    'ERROR_ANALYTICS',
    'SECURITY_ANALYTICS',
    'BUSINESS_METRICS',
    'CUSTOM_METRICS'
  ];

  useEffect(() => {
    loadAnalyticsData();
    loadInsights();
  }, []);

  const loadAnalyticsData = async () => {
    try {
      setLoading(true);
      const response = await analyticsService.getAnalyticsData();
      setAnalyticsData(response.content);
    } catch (err) {
      setError('Failed to load analytics data');
    } finally {
      setLoading(false);
    }
  };

  const loadInsights = async () => {
    try {
      const insightsData = await analyticsService.getAnalyticsInsights();
      setInsights(insightsData);
    } catch (err) {
      setError('Failed to load analytics insights');
    }
  };

  const handleCreateAnalyticsData = async () => {
    try {
      if (editingData) {
        // Update logic would go here
      } else {
        await analyticsService.createAnalyticsData(formData);
      }
      setOpenDialog(false);
      setEditingData(null);
      setFormData({
        analyticsType: '',
        metricName: '',
        metricValue: 0,
        dimensions: '',
        tags: '',
        userId: '',
        sessionId: '',
        ipAddress: '',
        userAgent: ''
      });
      loadAnalyticsData();
    } catch (err) {
      setError('Failed to save analytics data');
    }
  };

  const handleEditData = (data: AnalyticsData) => {
    setEditingData(data);
    setFormData(data);
    setOpenDialog(true);
  };

  const handleDeleteData = async (id: number) => {
    if (window.confirm('Are you sure you want to delete this analytics data?')) {
      try {
        // Delete logic would go here
        loadAnalyticsData();
      } catch (err) {
        setError('Failed to delete analytics data');
      }
    }
  };

  const getAnalyticsTypeColor = (type: string) => {
    switch (type) {
      case 'USER_BEHAVIOR':
        return 'primary';
      case 'SYSTEM_PERFORMANCE':
        return 'success';
      case 'DOCUMENT_ANALYTICS':
        return 'info';
      case 'SEARCH_ANALYTICS':
        return 'warning';
      case 'ERROR_ANALYTICS':
        return 'error';
      default:
        return 'default';
    }
  };

  const getTrendIcon = (trend: string) => {
    switch (trend) {
      case 'UP':
        return <TrendingUpIcon color="success" />;
      case 'DOWN':
        return <TrendingDownIcon color="error" />;
      default:
        return <AssessmentIcon color="info" />;
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
          Advanced Analytics
        </Typography>
        {user?.role === 'ADMIN' && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setOpenDialog(true)}
          >
            Add Analytics Data
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
                Total Data Points
              </Typography>
              <Typography variant="h4">
                {analyticsData.length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Unique Metrics
              </Typography>
              <Typography variant="h4" color="primary.main">
                {new Set(analyticsData.map(d => d.metricName)).size}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Average Value
              </Typography>
              <Typography variant="h4" color="success.main">
                {insights?.averageValue?.toFixed(2) || '0.00'}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Trend
              </Typography>
              <Box display="flex" alignItems="center">
                {getTrendIcon(insights?.trend || 'STABLE')}
                <Typography variant="h6" sx={{ ml: 1 }}>
                  {insights?.trend || 'STABLE'}
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Analytics Tabs */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Tabs value={activeTab} onChange={(e, newValue) => setActiveTab(newValue)}>
                <Tab label="Data Overview" icon={<AssessmentIcon />} />
                <Tab label="Insights" icon={<InfoIcon />} />
                <Tab label="Trends" icon={<TimelineIcon />} />
              </Tabs>

              {activeTab === 0 && (
                <Box sx={{ mt: 2 }}>
                  <Typography variant="h6" gutterBottom>
                    Analytics Data
                  </Typography>
                  <TableContainer component={Paper}>
                    <Table>
                      <TableHead>
                        <TableRow>
                          <TableCell>Type</TableCell>
                          <TableCell>Metric Name</TableCell>
                          <TableCell>Value</TableCell>
                          <TableCell>User ID</TableCell>
                          <TableCell>Timestamp</TableCell>
                          <TableCell>Actions</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {analyticsData.map((data) => (
                          <TableRow key={data.id}>
                            <TableCell>
                              <Chip
                                label={data.analyticsType}
                                color={getAnalyticsTypeColor(data.analyticsType) as any}
                                size="small"
                              />
                            </TableCell>
                            <TableCell>
                              <Typography variant="subtitle2">
                                {data.metricName}
                              </Typography>
                            </TableCell>
                            <TableCell>
                              <Typography variant="body2">
                                {data.metricValue?.toFixed(2) || 'N/A'}
                              </Typography>
                            </TableCell>
                            <TableCell>
                              <Typography variant="body2">
                                {data.userId || 'System'}
                              </Typography>
                            </TableCell>
                            <TableCell>
                              <Typography variant="body2">
                                {data.timestamp
                                  ? new Date(data.timestamp).toLocaleString()
                                  : new Date(data.createdAt!).toLocaleString()
                                }
                              </Typography>
                            </TableCell>
                            <TableCell>
                              {user?.role === 'ADMIN' && (
                                <>
                                  <Tooltip title="Edit">
                                    <IconButton
                                      size="small"
                                      onClick={() => handleEditData(data)}
                                    >
                                      <EditIcon />
                                    </IconButton>
                                  </Tooltip>
                                  <Tooltip title="Delete">
                                    <IconButton
                                      size="small"
                                      onClick={() => handleDeleteData(data.id!)}
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
                </Box>
              )}

              {activeTab === 1 && insights && (
                <Box sx={{ mt: 2 }}>
                  <Typography variant="h6" gutterBottom>
                    Analytics Insights
                  </Typography>
                  <Grid container spacing={2}>
                    <Grid item xs={12} md={6}>
                      <Card variant="outlined">
                        <CardContent>
                          <Typography variant="h6" gutterBottom>
                            Key Insights
                          </Typography>
                          <List>
                            {insights.insights.map((insight, index) => (
                              <ListItem key={index}>
                                <ListItemIcon>
                                  <InfoIcon color="primary" />
                                </ListItemIcon>
                                <ListItemText primary={insight} />
                              </ListItem>
                            ))}
                          </List>
                        </CardContent>
                      </Card>
                    </Grid>
                    <Grid item xs={12} md={6}>
                      <Card variant="outlined">
                        <CardContent>
                          <Typography variant="h6" gutterBottom>
                            Recommendations
                          </Typography>
                          <List>
                            {insights.recommendations.map((recommendation, index) => (
                              <ListItem key={index}>
                                <ListItemIcon>
                                  <CheckCircleIcon color="success" />
                                </ListItemIcon>
                                <ListItemText primary={recommendation} />
                              </ListItem>
                            ))}
                          </List>
                        </CardContent>
                      </Card>
                    </Grid>
                  </Grid>
                </Box>
              )}

              {activeTab === 2 && (
                <Box sx={{ mt: 2 }}>
                  <Typography variant="h6" gutterBottom>
                    Analytics Trends
                  </Typography>
                  <Card variant="outlined">
                    <CardContent>
                      <Typography variant="body1" color="textSecondary">
                        Trend analysis will be displayed here. This would typically include charts
                        showing metrics over time, comparison with previous periods, and
                        predictive analytics.
                      </Typography>
                    </CardContent>
                  </Card>
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Add/Edit Analytics Data Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          {editingData ? 'Edit Analytics Data' : 'Add New Analytics Data'}
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth required>
                <InputLabel>Analytics Type</InputLabel>
                <Select
                  value={formData.analyticsType}
                  onChange={(e) => setFormData({ ...formData, analyticsType: e.target.value })}
                >
                  {analyticsTypes.map((type) => (
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
                label="Metric Name"
                value={formData.metricName}
                onChange={(e) => setFormData({ ...formData, metricName: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Metric Value"
                type="number"
                value={formData.metricValue}
                onChange={(e) => setFormData({ ...formData, metricValue: parseFloat(e.target.value) })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="User ID"
                value={formData.userId}
                onChange={(e) => setFormData({ ...formData, userId: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Dimensions (JSON)"
                multiline
                rows={3}
                value={formData.dimensions}
                onChange={(e) => setFormData({ ...formData, dimensions: e.target.value })}
                placeholder='{"department": "HR", "location": "NYC"}'
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Tags (JSON)"
                multiline
                rows={2}
                value={formData.tags}
                onChange={(e) => setFormData({ ...formData, tags: e.target.value })}
                placeholder='["frequent_user", "hr_department"]'
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button onClick={handleCreateAnalyticsData} variant="contained">
            {editingData ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default AdvancedAnalytics;
