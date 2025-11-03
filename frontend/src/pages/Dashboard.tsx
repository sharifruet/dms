import React, { useState, useEffect } from 'react';
import {
  Box,
  Grid,
  Typography,
  Card,
  CardContent,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import {
  Description as DocumentsIcon,
  Search as SearchIcon,
  Assessment as ReportsIcon,
  Notifications as NotificationsIcon,
} from '@mui/icons-material';
import ActivityFeed from '../components/ActivityFeed';
import LineChartWidget from '../components/charts/LineChartWidget';
import BarChartWidget from '../components/charts/BarChartWidget';
import PieChartWidget from '../components/charts/PieChartWidget';

const Dashboard: React.FC = () => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  const stats = [
    {
      title: 'Total Documents',
      value: '1,234',
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

  const documentTypesData = [
    { name: 'Contracts', value: 420 },
    { name: 'Invoices', value: 380 },
    { name: 'Reports', value: 320 },
    { name: 'Certificates', value: 250 },
    { name: 'Others', value: 180 },
  ];

  const chartColors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];

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
          <PieChartWidget
            title="Document Types"
            data={documentTypesData}
            colors={chartColors}
            height={300}
          />
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
