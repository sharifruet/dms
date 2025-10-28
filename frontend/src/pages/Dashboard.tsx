import React from 'react';
import {
  Box,
  Grid,
  Typography,
  Card,
  CardContent,
} from '@mui/material';
import {
  Description as DocumentsIcon,
  Search as SearchIcon,
  Assessment as ReportsIcon,
  Notifications as NotificationsIcon,
} from '@mui/icons-material';

const Dashboard: React.FC = () => {
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

  return (
    <Box sx={{ p: 4 }}>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography
          variant="h4"
          sx={{
            fontWeight: 700,
            fontSize: '1.875rem',
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
            fontSize: '0.9375rem',
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

      {/* Content Cards */}
      <Grid container spacing={3}>
        <Grid item xs={12} md={8}>
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
                Recent Activity
              </Typography>
              <Box
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  py: 8,
                  color: '#9ca3af',
                }}
              >
                <Typography variant="body2">No recent activity</Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>

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
