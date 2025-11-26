import React, { useState, useEffect } from 'react';
import {
  Box,
  Grid,
  Typography,
  Card,
  CardContent,
  useMediaQuery,
  useTheme,
  LinearProgress,
  Paper,
  CircularProgress,
} from '@mui/material';
import {
  Description as DocumentsIcon,
  Search as SearchIcon,
  Assessment as ReportsIcon,
  Notifications as NotificationsIcon,
  Gavel as TenderIcon,
} from '@mui/icons-material';
import ActivityFeed from '../components/ActivityFeed';
import LineChartWidget from '../components/charts/LineChartWidget';
import BarChartWidget from '../components/charts/BarChartWidget';
import PieChartWidget from '../components/charts/PieChartWidget';
import { financeService, AppBudgetSummary } from '../services/financeService';
import { documentService } from '../services/documentService';

const Dashboard: React.FC = () => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [budgetData, setBudgetData] = useState<{
    totalBudget: number;
    totalBilled: number;
    remaining: number;
    utilizationPct: number;
  } | null>(null);
  const [perAppBudget, setPerAppBudget] = useState<AppBudgetSummary[]>([]);
  const [loadingBudget, setLoadingBudget] = useState(true);
  const [documentStats, setDocumentStats] = useState<{
    totalDocuments: number;
    activeDocuments: number;
    archivedDocuments: number;
    deletedDocuments: number;
  } | null>(null);
  const [loadingDocumentStats, setLoadingDocumentStats] = useState(true);
  const [tenderStats, setTenderStats] = useState<{
    totalTenders: number;
    liveTenders: number;
    closedTenders: number;
    draftTenders: number;
  } | null>(null);
  const [loadingTenderStats, setLoadingTenderStats] = useState(true);
  const [documentTypesData, setDocumentTypesData] = useState<Array<{ name: string; value: number }>>([]);
  const [loadingDocumentTypes, setLoadingDocumentTypes] = useState(true);

  const formatNumber = (num: number): string => {
    return new Intl.NumberFormat('en-US').format(num);
  };

  const stats = [
    {
      title: 'Total Documents',
      value: loadingDocumentStats ? '...' : formatNumber(documentStats?.totalDocuments || 0),
      icon: <DocumentsIcon sx={{ fontSize: 32 }} />,
      color: '#3b82f6',
      bgColor: '#eff6ff',
    },
    {
      title: 'Recent Searches',
      value: '89',
      icon: <SearchIcon sx={{ fontSize: 32 }} />,
      color: '#10b981',
      bgColor: '#ecfdf5',
    },
    {
      title: 'Active Reports',
      value: '12',
      icon: <ReportsIcon sx={{ fontSize: 32 }} />,
      color: '#8b5cf6',
      bgColor: '#f5f3ff',
    },
    {
      title: 'Notifications',
      value: '5',
      icon: <NotificationsIcon sx={{ fontSize: 32 }} />,
      color: '#f59e0b',
      bgColor: '#fffbeb',
    },
  ];

  const tenderStatsCards = [
    {
      title: 'Total Tenders',
      value: loadingTenderStats ? '...' : formatNumber(tenderStats?.totalTenders || 0),
      icon: <TenderIcon sx={{ fontSize: 32 }} />,
      color: '#ef4444',
      bgColor: '#fef2f2',
    },
    {
      title: 'Live Tenders',
      value: loadingTenderStats ? '...' : formatNumber(tenderStats?.liveTenders || 0),
      icon: <TenderIcon sx={{ fontSize: 32 }} />,
      color: '#10b981',
      bgColor: '#ecfdf5',
    },
    {
      title: 'Closed Tenders',
      value: loadingTenderStats ? '...' : formatNumber(tenderStats?.closedTenders || 0),
      icon: <TenderIcon sx={{ fontSize: 32 }} />,
      color: '#6b7280',
      bgColor: '#f9fafb',
    },
  ];

  // Sample data for charts
  const documentTrendsData = [
    { name: 'Jan', uploaded: 65, processed: 58 },
    { name: 'Feb', uploaded: 78, processed: 72 },
    { name: 'Mar', uploaded: 90, processed: 85 },
    { name: 'Apr', uploaded: 81, processed: 78 },
    { name: 'May', uploaded: 95, processed: 90 },
    { name: 'Jun', uploaded: 112, processed: 105 },
  ];

  const departmentData = [
    { name: 'Finance', documents: 450 },
    { name: 'HR', documents: 320 },
    { name: 'IT', documents: 280 },
    { name: 'Operations', documents: 220 },
    { name: 'Legal', documents: 180 },
  ];

  // documentTypesData is now loaded from API - see useEffect below

  const chartColors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];

  useEffect(() => {
    const loadBudgetData = async () => {
      try {
        setLoadingBudget(true);
        const [summary, byApp] = await Promise.all([
          financeService.getBudgetSummary(),
          financeService.getBudgetByApp(),
        ]);
        setBudgetData(summary);
        setPerAppBudget(byApp);
      } catch (error) {
        console.error('Failed to load budget data:', error);
      } finally {
        setLoadingBudget(false);
      }
    };
    loadBudgetData();
  }, []);

  useEffect(() => {
    const loadDocumentStats = async () => {
      try {
        setLoadingDocumentStats(true);
        const data = await documentService.getDocumentStatistics();
        setDocumentStats(data);
      } catch (error) {
        console.error('Failed to load document statistics:', error);
      } finally {
        setLoadingDocumentStats(false);
      }
    };
    loadDocumentStats();
  }, []);

  useEffect(() => {
    const loadTenderStats = async () => {
      try {
        setLoadingTenderStats(true);
        const data = await documentService.getTenderStatistics();
        setTenderStats(data);
      } catch (error) {
        console.error('Failed to load tender statistics:', error);
      } finally {
        setLoadingTenderStats(false);
      }
    };
    loadTenderStats();
  }, []);

  useEffect(() => {
    const loadDocumentTypes = async () => {
      try {
        setLoadingDocumentTypes(true);
        const typeCounts = await documentService.getDocumentStatisticsByType();
        
        // Transform the data into the format expected by PieChartWidget
        // Sort by count (descending) and take top 5, then group the rest as "Others"
        const sortedTypes = Object.entries(typeCounts)
          .map(([name, value]) => ({ name, value }))
          .sort((a, b) => b.value - a.value);
        
        const topTypes = sortedTypes.slice(0, 5);
        const othersCount = sortedTypes.slice(5).reduce((sum, item) => sum + item.value, 0);
        
        const formattedData = topTypes.map(item => ({
          name: item.name.replace(/_/g, ' '), // Replace underscores with spaces for better display
          value: item.value,
        }));
        
        if (othersCount > 0) {
          formattedData.push({ name: 'Others', value: othersCount });
        }
        
        setDocumentTypesData(formattedData);
      } catch (error) {
        console.error('Failed to load document types statistics:', error);
        // Fallback to empty array on error
        setDocumentTypesData([]);
      } finally {
        setLoadingDocumentTypes(false);
      }
    };
    loadDocumentTypes();
  }, []);

  const formatCurrency = (amount: number): string => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'BDT',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const formatFiscalYear = (year: number): string => {
    const nextYear = (year + 1) % 100;
    return `${year}-${nextYear.toString().padStart(2, '0')}`;
  };

  return (
    <Box sx={{ p: { xs: 2, md: 4 } }}>
      {/* Header */}
      <Box sx={{ mb: { xs: 3, md: 4 } }}>
        <Typography
          variant="h4"
          sx={{
            fontWeight: 700,
            fontSize: { xs: '1.5rem', md: '1.875rem' },
            color: '#111827',
            mb: 1,
            letterSpacing: '-0.02em',
          }}
        >
          Dashboard
        </Typography>
        <Typography
          variant="body2"
          sx={{
            color: '#6b7280',
            fontSize: { xs: '0.875rem', md: '0.9375rem' },
          }}
        >
          Welcome back! Here's what's happening with your documents.
        </Typography>
      </Box>

      {/* Stats Grid */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        {stats.map((stat, index) => (
          <Grid item xs={12} sm={6} md={3} key={index}>
            <Card
              sx={{
                borderRadius: 3,
                boxShadow: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.12)',
                border: '1px solid #f3f4f6',
                transition: 'all 0.2s ease',
                '&:hover': {
                  boxShadow: '0 4px 12px rgba(0,0,0,0.1), 0 2px 4px rgba(0,0,0,0.08)',
                  transform: 'translateY(-2px)',
                },
              }}
            >
              <CardContent sx={{ p: 3 }}>
                <Box
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    mb: 2,
                  }}
                >
                  <Box
                    sx={{
                      width: 48,
                      height: 48,
                      borderRadius: 2,
                      backgroundColor: stat.bgColor,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: stat.color,
                    }}
                  >
                    {stat.icon}
                  </Box>
                </Box>
                <Typography
                  variant="h4"
                  sx={{
                    fontWeight: 700,
                    fontSize: '1.875rem',
                    color: '#111827',
                    mb: 0.5,
                  }}
                >
                  {stat.value}
                </Typography>
                <Typography
                  variant="body2"
                  sx={{
                    color: '#6b7280',
                    fontSize: '0.875rem',
                    fontWeight: 500,
                  }}
                >
                  {stat.title}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {/* Tender Statistics Section */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12}>
          <Typography
            variant="h5"
            sx={{
              fontWeight: 600,
              fontSize: '1.25rem',
              color: '#111827',
              mb: 2,
            }}
          >
            Tender Statistics
          </Typography>
        </Grid>
        {tenderStatsCards.map((stat, index) => (
          <Grid item xs={12} sm={6} md={4} key={`tender-${index}`}>
            <Card
              sx={{
                borderRadius: 3,
                boxShadow: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.12)',
                border: '1px solid #f3f4f6',
                transition: 'all 0.2s ease',
                '&:hover': {
                  boxShadow: '0 4px 12px rgba(0,0,0,0.1), 0 2px 4px rgba(0,0,0,0.08)',
                  transform: 'translateY(-2px)',
                },
              }}
            >
              <CardContent sx={{ p: 3 }}>
                <Box
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    mb: 2,
                  }}
                >
                  <Box
                    sx={{
                      width: 48,
                      height: 48,
                      borderRadius: 2,
                      backgroundColor: stat.bgColor,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: stat.color,
                    }}
                  >
                    {stat.icon}
                  </Box>
                </Box>
                <Typography
                  variant="h4"
                  sx={{
                    fontWeight: 700,
                    fontSize: '1.875rem',
                    color: '#111827',
                    mb: 0.5,
                  }}
                >
                  {stat.value}
                </Typography>
                <Typography
                  variant="body2"
                  sx={{
                    color: '#6b7280',
                    fontSize: '0.875rem',
                    fontWeight: 500,
                  }}
                >
                  {stat.title}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {/* Budget Summary Section - Per APP */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12}>
          <Typography
            variant="h5"
            sx={{
              fontWeight: 600,
              fontSize: '1.25rem',
              color: '#111827',
              mb: 2,
            }}
          >
            Budget vs Billed by APP
          </Typography>
        </Grid>

        {loadingBudget ? (
          <Grid item xs={12}>
            <Box display="flex" justifyContent="center" alignItems="center" minHeight={300}>
              <CircularProgress />
            </Box>
          </Grid>
        ) : perAppBudget.length === 0 ? (
          <Grid item xs={12}>
            <Typography variant="body2" color="text.secondary">
              No APP budget data available
            </Typography>
          </Grid>
        ) : (
          perAppBudget.map((app) => {
            const pieData = [
              { name: 'Remaining Budget', value: Math.max(0, app.remaining) },
              { name: 'Billed Amount', value: app.totalBilled },
            ];

            const utilization = Number(app.utilizationPct || 0);

            return (
              <Grid item xs={12} md={6} key={app.appId}>
                <Paper
                  elevation={0}
                  sx={{
                    p: 3,
                    borderRadius: 3,
                    border: '1px solid #e5e7eb',
                    height: '100%',
                  }}
                >
                  <Typography
                    variant="subtitle1"
                    sx={{
                      fontWeight: 600,
                      fontSize: '1rem',
                      color: '#1f2937',
                      mb: 0.5,
                    }}
                  >
                    APP {app.appId} - FY {formatFiscalYear(app.fiscalYear)}
                  </Typography>
                  <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 2 }}>
                    Installment {app.releaseInstallmentNo ?? '-'} {app.allocationType ? `(${app.allocationType})` : ''}
                  </Typography>

                  <Grid container spacing={2}>
                    <Grid item xs={12} sm={6}>
                      <PieChartWidget
                        title=""
                        data={pieData}
                        colors={['#10b981', '#3b82f6']}
                        height={220}
                      />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <Box>
                        <Box sx={{ mb: 2 }}>
                          <Box display="flex" justifyContent="space-between" alignItems="center" mb={0.5}>
                            <Typography variant="body2" color="text.secondary">
                              Billed Amount
                            </Typography>
                            <Typography variant="body2" fontWeight={600}>
                              {formatCurrency(app.totalBilled)}
                            </Typography>
                          </Box>
                          <Box display="flex" justifyContent="space-between" alignItems="center" mb={0.5}>
                            <Typography variant="body2" color="text.secondary">
                              Total Budget
                            </Typography>
                            <Typography variant="body2" fontWeight={600}>
                              {formatCurrency(app.allocationAmount)}
                            </Typography>
                          </Box>
                          <Box display="flex" justifyContent="space-between" alignItems="center">
                            <Typography variant="body2" color="text.secondary">
                              Remaining
                            </Typography>
                            <Typography
                              variant="body2"
                              fontWeight={600}
                              color={app.remaining >= 0 ? 'success.main' : 'error.main'}
                            >
                              {formatCurrency(app.remaining)}
                            </Typography>
                          </Box>
                        </Box>

                        <Box sx={{ mb: 1.5 }}>
                          <Box display="flex" justifyContent="space-between" alignItems="center" mb={0.5}>
                            <Typography variant="body2" color="text.secondary">
                              Utilization
                            </Typography>
                            <Typography variant="body2" fontWeight={600}>
                              {utilization.toFixed(2)}%
                            </Typography>
                          </Box>
                          <LinearProgress
                            variant="determinate"
                            value={Math.min(100, utilization)}
                            sx={{
                              height: 16,
                              borderRadius: 2,
                              backgroundColor: '#e5e7eb',
                              '& .MuiLinearProgress-bar': {
                                borderRadius: 2,
                                backgroundColor:
                                  utilization > 90
                                    ? '#ef4444'
                                    : utilization > 75
                                    ? '#f59e0b'
                                    : '#10b981',
                              },
                            }}
                          />
                        </Box>

                        <Typography variant="caption" color="text.secondary">
                          {formatCurrency(app.totalBilled)} of {formatCurrency(app.allocationAmount)} billed
                        </Typography>
                      </Box>
                    </Grid>
                  </Grid>
                </Paper>
              </Grid>
            );
          })
        )}
      </Grid>

      {/* Charts Grid */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} lg={8}>
          <LineChartWidget
            title="Document Trends"
            data={documentTrendsData}
            lines={[
              { dataKey: 'uploaded', color: '#3b82f6', name: 'Uploaded' },
              { dataKey: 'processed', color: '#10b981', name: 'Processed' },
            ]}
            height={300}
          />
        </Grid>
        <Grid item xs={12} lg={4}>
          {loadingDocumentTypes ? (
            <Box display="flex" justifyContent="center" alignItems="center" minHeight={300}>
              <CircularProgress />
            </Box>
          ) : documentTypesData.length > 0 ? (
            <PieChartWidget
              title="Document Types"
              data={documentTypesData}
              colors={chartColors}
              height={300}
            />
          ) : (
            <Paper
              elevation={0}
              sx={{
                p: 3,
                borderRadius: 3,
                border: '1px solid #e5e7eb',
                height: '100%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Typography variant="body2" color="text.secondary">
                No document type data available
              </Typography>
            </Paper>
          )}
        </Grid>
      </Grid>

      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} lg={6}>
          <BarChartWidget
            title="Documents by Department"
            data={departmentData}
            bars={[
              { dataKey: 'documents', color: '#3b82f6', name: 'Documents' },
            ]}
            height={300}
          />
        </Grid>
        <Grid item xs={12} lg={6}>
          <Box sx={{ height: isMobile ? 500 : 345 }}>
            <ActivityFeed />
          </Box>
        </Grid>
      </Grid>

      {/* Content Cards */}
      <Grid container spacing={3}>
        <Grid item xs={12} md={4}>
          <Card
            sx={{
              borderRadius: 3,
              boxShadow: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.12)',
              border: '1px solid #f3f4f6',
            }}
          >
            <CardContent sx={{ p: 3 }}>
              <Typography
                variant="h6"
                sx={{
                  fontWeight: 600,
                  fontSize: '1.125rem',
                  color: '#111827',
                  mb: 2,
                }}
              >
                Quick Actions
              </Typography>
              <Box
                sx={{
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 1.5,
                }}
              >
                {['Upload Document', 'Create Report', 'View Analytics', 'Manage Users'].map(
                  (action, index) => (
                    <Box
                      key={index}
                      sx={{
                        p: 1.5,
                        borderRadius: 2,
                        border: '1px solid #e5e7eb',
                        cursor: 'pointer',
                        transition: 'all 0.15s ease',
                        '&:hover': {
                          backgroundColor: '#f9fafb',
                          borderColor: '#d1d5db',
                        },
                      }}
                    >
                      <Typography
                        variant="body2"
                        sx={{
                          fontWeight: 500,
                          color: '#374151',
                          fontSize: '0.875rem',
                        }}
                      >
                        {action}
                      </Typography>
                    </Box>
                  )
                )}
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Dashboard;
