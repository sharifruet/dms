import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Box,
  Drawer,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
  useMediaQuery,
  useTheme,
  AppBar,
  Toolbar,
  Badge,
} from '@mui/material';
import {
  Menu as MenuIcon,
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
  Close as CloseIcon,
} from '@mui/icons-material';
import { useAppSelector, useAppDispatch } from '../hooks/redux';
import { logout } from '../store/slices/authSlice';
import NotificationBell from './NotificationBell';

interface NavItem {
  label: string;
  path: string;
  icon: React.ReactNode;
  role?: string;
}

const MobileSidebar: React.FC = () => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [mobileOpen, setMobileOpen] = useState(false);
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
    // { label: 'Reports', path: '/reports', icon: <ReportsIcon /> },
    // { label: 'Custom Dashboards', path: '/dashboard-management', icon: <AnalyticsIcon /> },
    { label: 'Workflows', path: '/workflows', icon: <WorkflowsIcon /> },
    { label: 'Versioning', path: '/versioning', icon: <VersioningIcon /> },
    // { label: 'Analytics', path: '/analytics', icon: <AnalyticsIcon /> },
    // { label: 'ML Models', path: '/ml', icon: <MlIcon /> },
    // { label: 'Integrations', path: '/integrations', icon: <IntegrationsIcon /> },
    { label: 'Health', path: '/health', icon: <HealthIcon /> },
    { label: 'Users', path: '/users', icon: <UsersIcon />, role: 'ADMIN' },
  ];

  const filteredNavItems = navItems.filter(
    (item) => !item.role || user?.role === item.role
  );

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const handleNavigation = (path: string) => {
    navigate(path);
    if (isMobile) {
      setMobileOpen(false);
    }
  };

  const isActive = (path: string) => {
    return location.pathname === path;
  };

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
  };

  const drawerContent = (
    <Box
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        backgroundColor: '#fafbfc',
      }}
    >
      {/* Logo & Close Button (Mobile) */}
      <Box
        sx={{
          px: 3,
          py: 2.5,
          borderBottom: '1px solid #e5e7eb',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <Typography
          variant="h6"
          sx={{
            fontWeight: 600,
            color: '#1f2937',
            fontSize: '1.25rem',
            letterSpacing: '-0.02em',
            cursor: 'pointer',
            '&:hover': {
              color: '#3b82f6',
            },
          }}
          onClick={() => handleNavigation('/dashboard')}
        >
          Document Manager
        </Typography>
        {isMobile && (
          <IconButton onClick={handleDrawerToggle} size="small">
            <CloseIcon />
          </IconButton>
        )}
      </Box>

      {/* Navigation Items */}
      <List sx={{ px: 1.5, py: 1.5, flex: 1 }}>
        {filteredNavItems.map((item) => (
          <ListItem key={item.path} disablePadding sx={{ mb: 0.5 }}>
            <ListItemButton
              onClick={() => handleNavigation(item.path)}
              sx={{
                borderRadius: 2,
                py: 1.5,
                px: 1.5,
                minHeight: 48,
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
                  fontSize: '0.9375rem',
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

  if (!isMobile) {
    // Desktop: Fixed Sidebar
    return (
      <Box
        sx={{
          width: 260,
          height: '100vh',
          position: 'fixed',
          left: 0,
          top: 0,
          borderRight: '1px solid #e5e7eb',
        }}
      >
        {drawerContent}
      </Box>
    );
  }

  // Mobile: AppBar + Drawer
  return (
    <>
      <AppBar
        position="fixed"
        elevation={0}
        sx={{
          backgroundColor: '#ffffff',
          borderBottom: '1px solid #e5e7eb',
        }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            edge="start"
            onClick={handleDrawerToggle}
            sx={{ mr: 2, color: '#1f2937' }}
          >
            <MenuIcon />
          </IconButton>
          <Typography
            variant="h6"
            noWrap
            component="div"
            sx={{
              flexGrow: 1,
              fontWeight: 600,
              color: '#1f2937',
              fontSize: '1.125rem',
            }}
          >
            Document Manager
          </Typography>
          <NotificationBell />
        </Toolbar>
      </AppBar>
      
      {/* Mobile Drawer */}
      <Drawer
        variant="temporary"
        open={mobileOpen}
        onClose={handleDrawerToggle}
        ModalProps={{
          keepMounted: true, // Better open performance on mobile
        }}
        sx={{
          display: { xs: 'block', md: 'none' },
          '& .MuiDrawer-paper': {
            boxSizing: 'border-box',
            width: 280,
          },
        }}
      >
        {drawerContent}
      </Drawer>

      {/* Spacer for AppBar */}
      <Toolbar />
    </>
  );
};

export default MobileSidebar;

