import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Box,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
  IconButton,
} from '@mui/material';
import {
  Dashboard as DashboardIcon,
  Description as DocumentsIcon,
  Search as SearchIcon,
  Notifications as NotificationsIcon,
  CalendarToday as ExpiryIcon,
  Assessment as ReportsIcon,
  AccountTree as WorkflowsIcon,
  History as VersioningIcon,
  Settings as IntegrationsIcon,
  Analytics as AnalyticsIcon,
  Psychology as MlIcon,
  HealthAndSafety as HealthIcon,
  People as UsersIcon,
  Logout as LogoutIcon,
} from '@mui/icons-material';
import { useAppSelector, useAppDispatch } from '../hooks/redux';
import { logout } from '../store/slices/authSlice';

interface NavItem {
  label: string;
  path: string;
  icon: React.ReactNode;
  role?: string;
}

const Sidebar: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useAppDispatch();
  const { user } = useAppSelector((state) => state.auth);

  const navItems: NavItem[] = [
    { label: 'Dashboard', path: '/dashboard', icon: <DashboardIcon /> },
    { label: 'Documents', path: '/documents', icon: <DocumentsIcon /> },
    { label: 'Search', path: '/search', icon: <SearchIcon /> },
    { label: 'Notifications', path: '/notifications', icon: <NotificationsIcon /> },
    { label: 'Expiry Tracking', path: '/expiry-tracking', icon: <ExpiryIcon /> },
    { label: 'Reports', path: '/reports', icon: <ReportsIcon /> },
    { label: 'Dashboards', path: '/dashboard-management', icon: <AnalyticsIcon /> },
    { label: 'Workflows', path: '/workflows', icon: <WorkflowsIcon /> },
    { label: 'Versioning', path: '/versioning', icon: <VersioningIcon /> },
    { label: 'Analytics', path: '/analytics', icon: <AnalyticsIcon /> },
    { label: 'ML Models', path: '/ml', icon: <MlIcon /> },
    { label: 'Integrations', path: '/integrations', icon: <IntegrationsIcon /> },
    { label: 'Health', path: '/health', icon: <HealthIcon /> },
    { label: 'Users', path: '/users', icon: <UsersIcon />, role: 'ADMIN' },
  ];

  const filteredNavItems = navItems.filter(
    (item) => !item.role || user?.role === item.role
  );

  const handleNavigation = (path: string) => {
    navigate(path);
  };

  const isActive = (path: string) => {
    return location.pathname === path;
  };

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
  };

  return (
    <Box
      sx={{
        width: 260,
        height: '100vh',
        backgroundColor: '#fafbfc',
        borderRight: '1px solid #e5e7eb',
        display: 'flex',
        flexDirection: 'column',
        position: 'fixed',
        left: 0,
        top: 0,
        overflow: 'auto',
      }}
    >
      {/* Logo */}
      <Box
        sx={{
          px: 3,
          py: 2.5,
          borderBottom: '1px solid #e5e7eb',
          cursor: 'pointer',
          '&:hover': {
            backgroundColor: '#f9fafb',
          },
        }}
        onClick={() => navigate('/dashboard')}
      >
        <Typography
          variant="h6"
          sx={{
            fontWeight: 600,
            color: '#1f2937',
            fontSize: '1.25rem',
            letterSpacing: '-0.02em',
          }}
        >
          Document Manager
        </Typography>
      </Box>

      {/* Navigation Items */}
      <List sx={{ px: 1.5, py: 1.5, flex: 1 }}>
        {filteredNavItems.map((item) => (
          <ListItem key={item.path} disablePadding sx={{ mb: 0.5 }}>
            <ListItemButton
              onClick={() => handleNavigation(item.path)}
              sx={{
                borderRadius: 2,
                py: 1,
                px: 1.5,
                minHeight: 40,
                backgroundColor: isActive(item.path) ? '#f0f4ff' : 'transparent',
                '&:hover': {
                  backgroundColor: isActive(item.path) ? '#e8eeff' : '#f3f4f6',
                },
                transition: 'all 0.15s ease',
              }}
            >
              <ListItemIcon
                sx={{
                  minWidth: 40,
                  color: isActive(item.path) ? '#3b82f6' : '#6b7280',
                }}
              >
                {item.icon}
              </ListItemIcon>
              <ListItemText
                primary={item.label}
                primaryTypographyProps={{
                  fontSize: '0.875rem',
                  fontWeight: isActive(item.path) ? 600 : 500,
                  color: isActive(item.path) ? '#1e40af' : '#374151',
                }}
              />
            </ListItemButton>
          </ListItem>
        ))}
      </List>

      {/* User Info */}
      <Box
        sx={{
          px: 2,
          py: 2,
          borderTop: '1px solid #e5e7eb',
          backgroundColor: '#ffffff',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <Box>
          <Typography
            variant="body2"
            sx={{
              fontWeight: 600,
              color: '#1f2937',
              fontSize: '0.875rem',
            }}
          >
            {user?.username || 'User'}
          </Typography>
          <Typography
            variant="caption"
            sx={{
              color: '#9ca3af',
              fontSize: '0.75rem',
              textTransform: 'capitalize',
            }}
          >
            {user?.role?.toLowerCase() || 'User'}
          </Typography>
        </Box>
        <IconButton
          onClick={handleLogout}
          size="small"
          sx={{
            color: '#6b7280',
            '&:hover': {
              backgroundColor: '#f3f4f6',
              color: '#ef4444',
            },
          }}
          title="Logout"
        >
          <LogoutIcon fontSize="small" />
        </IconButton>
      </Box>
    </Box>
  );
};

export default Sidebar;
