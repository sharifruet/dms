import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Grid,
  Alert,
  CircularProgress,
  Tabs,
  Tab,
  IconButton,
  Tooltip,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Chip,
  LinearProgress
} from '@mui/material';
import {
  Dashboard as DashboardIcon,
  Assessment as AssessmentIcon,
  TrendingUp as TrendingUpIcon,
  TrendingDown as TrendingDownIcon,
  Warning as WarningIcon,
  CheckCircle as CheckCircleIcon,
  Error as ErrorIcon,
  Info as InfoIcon,
  Refresh as RefreshIcon,
  Download as DownloadIcon,
  MoreVert as MoreVertIcon,
  Storage as StorageIcon,
  People as PeopleIcon,
  Description as DescriptionIcon,
  Schedule as ScheduleIcon,
  Security as SecurityIcon,
  Speed as SpeedIcon
} from '@mui/icons-material';
import dashboardService, { Dashboard, DashboardType } from '../services/dashboardService';

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
      id={`dashboard-tabpanel-${index}`}
      aria-labelledby={`dashboard-tab-${index}`}
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

const DashboardPage: React.FC = () => {
  const [dashboardData, setDashboardData] = useState<any>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [tabValue, setTabValue] = useState(0);
  const [lastRefresh, setLastRefresh] = useState<Date>(new Date());

  useEffect(() => {
    loadDashboardData();
  }, [tabValue]);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      let data;
      
      switch (tabValue) {
        case 0: // Executive
          data = await dashboardService.getExecutiveDashboardData();
          break;
        case 1: // Department
          data = await dashboardService.getDepartmentDashboardData('All');
          break;
        case 2: // User
          data = await dashboardService.getUserDashboardData();
          break;
        case 3: // System
          data = await dashboardService.getSystemDashboardData();
          break;
        case 4: // Compliance
          data = await dashboardService.getComplianceDashboardData();
          break;
        default:
          data = {};
      }
      
      setDashboardData(data);
      setLastRefresh(new Date());
    } catch (error) {
      setError('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = () => {
    loadDashboardData();
  };

  const getMetricIcon = (metricName: string) => {
    if (metricName.toLowerCase().includes('document')) return <DescriptionIcon />;
    if (metricName.toLowerCase().includes('user')) return <PeopleIcon />;
    if (metricName.toLowerCase().includes('storage')) return <StorageIcon />;
    if (metricName.toLowerCase().includes('expiry')) return <ScheduleIcon />;
    if (metricName.toLowerCase().includes('compliance')) return <SecurityIcon />;
    if (metricName.toLowerCase().includes('performance')) return <SpeedIcon />;
    return <AssessmentIcon />;
  };

  const getMetricColor = (metricName: string, value: number) => {
    if (metricName.toLowerCase().includes('error') || metricName.toLowerCase().includes('expired')) {
      return value > 0 ? 'error' : 'success';
    }
    if (metricName.toLowerCase().includes('uptime') || metricName.toLowerCase().includes('success')) {
      return value > 90 ? 'success' : value > 70 ? 'warning' : 'error';
    }
    return 'primary';
  };

  const formatValue = (value: any, metricName: string) => {
    if (typeof value === 'number') {
      if (metricName.toLowerCase().includes('rate') || metricName.toLowerCase().includes('percentage')) {
        return `${(value * 100).toFixed(1)}%`;
      }
      if (metricName.toLowerCase().includes('storage') || metricName.toLowerCase().includes('size')) {
        return `${value.toFixed(1)} MB`;
      }
      if (metricName.toLowerCase().includes('time')) {
        return `${value} ms`;
      }
      return value.toLocaleString();
    }
    return value;
  };

  const renderMetricCard = (title: string, value: any, subtitle?: string, trend?: number) => (
    <Card>
      <CardContent>
        <Box display="flex" alignItems="center" justifyContent="space-between">
          <Box>
            <Typography color="textSecondary" gutterBottom variant="h6">
              {title}
            </Typography>
            <Typography variant="h4" component="h2">
              {formatValue(value, title)}
            </Typography>
            {subtitle && (
              <Typography color="textSecondary" variant="body2">
                {subtitle}
              </Typography>
            )}
          </Box>
          <Box display="flex" alignItems="center">
            {getMetricIcon(title)}
            {trend !== undefined && (
              <Box ml={1}>
                {trend > 0 ? (
                  <TrendingUpIcon color="success" />
                ) : trend < 0 ? (
                  <TrendingDownIcon color="error" />
                ) : (
                  <TrendingUpIcon color="disabled" />
                )}
              </Box>
            )}
          </Box>
        </Box>
        {trend !== undefined && (
          <Box mt={2}>
            <LinearProgress
              variant="determinate"
              value={Math.abs(trend)}
              color={trend > 0 ? 'success' : trend < 0 ? 'error' : 'primary'}
            />
          </Box>
        )}
      </CardContent>
    </Card>
  );

  const renderExecutiveDashboard = () => (
    <Grid container spacing={3}>
      {/* Key Metrics */}
      <Grid item xs={12} sm={6} md={3}>
        {renderMetricCard('Total Documents', dashboardData.totalDocuments)}
      </Grid>
      <Grid item xs={12} sm={6} md={3}>
        {renderMetricCard('Total Users', dashboardData.totalUsers)}
      </Grid>
      <Grid item xs={12} sm={6} md={3}>
        {renderMetricCard('Active Tracking', dashboardData.activeTracking)}
      </Grid>
      <Grid item xs={12} sm={6} md={3}>
        {renderMetricCard('Unread Notifications', dashboardData.unreadNotifications)}
      </Grid>

      {/* System Health */}
      <Grid item xs={12} md={6}>
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              System Health
            </Typography>
            <Box display="flex" alignItems="center" mb={2}>
              <CheckCircleIcon color="success" sx={{ mr: 1 }} />
              <Typography variant="body1">System Uptime: {dashboardData.systemUptime} hours</Typography>
            </Box>
            <Box display="flex" alignItems="center" mb={2}>
              <SpeedIcon color="primary" sx={{ mr: 1 }} />
              <Typography variant="body1">Avg Response Time: {dashboardData.averageResponseTime} ms</Typography>
            </Box>
            <Box display="flex" alignItems="center">
              <ErrorIcon color="warning" sx={{ mr: 1 }} />
              <Typography variant="body1">Error Rate: {(dashboardData.errorRate * 100).toFixed(2)}%</Typography>
            </Box>
          </CardContent>
        </Card>
      </Grid>

      {/* Storage Usage */}
      <Grid item xs={12} md={6}>
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Storage Usage
            </Typography>
            <Box display="flex" alignItems="center" mb={2}>
              <StorageIcon color="primary" sx={{ mr: 1 }} />
              <Typography variant="body1">
                Estimated Storage: {dashboardData.estimatedStorageMB?.toFixed(1)} MB
              </Typography>
            </Box>
            <LinearProgress
              variant="determinate"
              value={23.8} // Placeholder
              sx={{ height: 8, borderRadius: 4 }}
            />
            <Typography variant="caption" color="textSecondary">
              23.8% of total capacity used
            </Typography>
          </CardContent>
        </Card>
      </Grid>

      {/* Recent Activity */}
      <Grid item xs={12}>
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Recent Activity Summary
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={4}>
                <Box textAlign="center">
                  <Typography variant="h4" color="primary">
                    {dashboardData.recentUploads || 0}
                  </Typography>
                  <Typography variant="body2" color="textSecondary">
                    Recent Uploads (30 days)
                  </Typography>
                </Box>
              </Grid>
              <Grid item xs={12} sm={4}>
                <Box textAlign="center">
                  <Typography variant="h4" color="primary">
                    {dashboardData.recentLogins || 0}
                  </Typography>
                  <Typography variant="body2" color="textSecondary">
                    Recent Logins (30 days)
                  </Typography>
                </Box>
              </Grid>
              <Grid item xs={12} sm={4}>
                <Box textAlign="center">
                  <Typography variant="h4" color="primary">
                    {dashboardData.expiringIn30Days || 0}
                  </Typography>
                  <Typography variant="body2" color="textSecondary">
                    Expiring in 30 days
                  </Typography>
                </Box>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      </Grid>
    </Grid>
  );

  const renderSystemDashboard = () => (
    <Grid container spacing={3}>
      {/* Performance Metrics */}
      <Grid item xs={12} sm={6} md={3}>
        {renderMetricCard('CPU Usage', dashboardData.cpuUsage, '%')}
      </Grid>
      <Grid item xs={12} sm={6} md={3}>
        {renderMetricCard('Memory Usage', dashboardData.memoryUsage, '%')}
      </Grid>
      <Grid item xs={12} sm={6} md={3}>
        {renderMetricCard('Disk Usage', dashboardData.diskUsage, '%')}
      </Grid>
      <Grid item xs={12} sm={6} md={3}>
        {renderMetricCard('Database Connections', dashboardData.databaseConnections)}
      </Grid>

      {/* System Status */}
      <Grid item xs={12} md={6}>
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              System Status
            </Typography>
            <Box display="flex" alignItems="center" mb={2}>
              <CheckCircleIcon color="success" sx={{ mr: 1 }} />
              <Typography variant="body1">Database: Online</Typography>
            </Box>
            <Box display="flex" alignItems="center" mb={2}>
              <CheckCircleIcon color="success" sx={{ mr: 1 }} />
              <Typography variant="body1">Cache: Online</Typography>
            </Box>
            <Box display="flex" alignItems="center" mb={2}>
              <CheckCircleIcon color="success" sx={{ mr: 1 }} />
              <Typography variant="body1">File Storage: Online</Typography>
            </Box>
            <Box display="flex" alignItems="center">
              <CheckCircleIcon color="success" sx={{ mr: 1 }} />
              <Typography variant="body1">Email Service: Online</Typography>
            </Box>
          </CardContent>
        </Card>
      </Grid>

      {/* Performance Charts Placeholder */}
      <Grid item xs={12} md={6}>
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Performance Trends
            </Typography>
            <Box display="flex" justifyContent="center" alignItems="center" minHeight="200px">
              <Typography color="textSecondary">
                Performance charts would be displayed here
              </Typography>
            </Box>
          </CardContent>
        </Card>
      </Grid>
    </Grid>
  );

  const renderComplianceDashboard = () => (
    <Grid container spacing={3}>
      {/* Compliance Metrics */}
      <Grid item xs={12} sm={6} md={3}>
        {renderMetricCard('Compliance Rate', dashboardData.complianceRate, '%')}
      </Grid>
      <Grid item xs={12} sm={6} md={3}>
        {renderMetricCard('User Compliance', dashboardData.userComplianceRate, '%')}
      </Grid>
      <Grid item xs={12} sm={6} md={3}>
        {renderMetricCard('Active Tracking', dashboardData.activeTracking)}
      </Grid>
      <Grid item xs={12} sm={6} md={3}>
        {renderMetricCard('Expired Documents', dashboardData.expiredDocuments)}
      </Grid>

      {/* Compliance Status */}
      <Grid item xs={12} md={6}>
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Compliance Status
            </Typography>
            <Box display="flex" alignItems="center" mb={2}>
              <CheckCircleIcon color="success" sx={{ mr: 1 }} />
              <Typography variant="body1">Document Retention: Compliant</Typography>
            </Box>
            <Box display="flex" alignItems="center" mb={2}>
              <WarningIcon color="warning" sx={{ mr: 1 }} />
              <Typography variant="body1">Expiry Alerts: {dashboardData.expiringIn7Days || 0} pending</Typography>
            </Box>
            <Box display="flex" alignItems="center" mb={2}>
              <CheckCircleIcon color="success" sx={{ mr: 1 }} />
              <Typography variant="body1">Audit Logging: Active</Typography>
            </Box>
            <Box display="flex" alignItems="center">
              <CheckCircleIcon color="success" sx={{ mr: 1 }} />
              <Typography variant="body1">Access Control: Enforced</Typography>
            </Box>
          </CardContent>
        </Card>
      </Grid>

      {/* Compliance Trends */}
      <Grid item xs={12} md={6}>
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Compliance Trends
            </Typography>
            <Box display="flex" justifyContent="center" alignItems="center" minHeight="200px">
              <Typography color="textSecondary">
                Compliance trend charts would be displayed here
              </Typography>
            </Box>
          </CardContent>
        </Card>
      </Grid>
    </Grid>
  );

  if (loading && Object.keys(dashboardData).length === 0) {
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
          Dashboard
        </Typography>
        <Box display="flex" alignItems="center">
          <Typography variant="body2" color="textSecondary" sx={{ mr: 2 }}>
            Last updated: {lastRefresh.toLocaleTimeString()}
          </Typography>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={handleRefresh}
            disabled={loading}
          >
            Refresh
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)}>
            <Tab label="Executive" />
            <Tab label="Department" />
            <Tab label="User" />
            <Tab label="System" />
            <Tab label="Compliance" />
          </Tabs>
        </Box>

        <TabPanel value={tabValue} index={0}>
          {renderExecutiveDashboard()}
        </TabPanel>

        <TabPanel value={tabValue} index={1}>
          <Typography variant="h6" mb={2}>
            Department Dashboard
          </Typography>
          <Typography color="textSecondary">
            Department-specific metrics and activities would be displayed here.
          </Typography>
        </TabPanel>

        <TabPanel value={tabValue} index={2}>
          <Typography variant="h6" mb={2}>
            User Dashboard
          </Typography>
          <Typography color="textSecondary">
            Personal dashboard for individual users would be displayed here.
          </Typography>
        </TabPanel>

        <TabPanel value={tabValue} index={3}>
          {renderSystemDashboard()}
        </TabPanel>

        <TabPanel value={tabValue} index={4}>
          {renderComplianceDashboard()}
        </TabPanel>
      </Card>
    </Box>
  );
};

export default DashboardPage;
