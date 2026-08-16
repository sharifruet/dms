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
  Divider,
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
  Inventory2 as AssetsIcon,
  Assignment as AssignmentsIcon,
  EditNote as FieldsIcon,
  Archive as ArchiveIcon,
  Inventory as StationeryIcon,
  TableChart as AppIcon,
  Receipt as BillIcon,
} from '@mui/icons-material';
import { useAppSelector, useAppDispatch } from '../hooks/redux';
import { logout } from '../store/slices/authSlice';
import NotificationBell from './NotificationBell';

interface NavItem {
  label: string;
  path: string;
  icon: React.ReactNode;
  role?: string;
  /** Caption rendered above this item, starting a new group. */
  section?: string;
}

const Sidebar: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useAppDispatch();
  const { user } = useAppSelector((state) => state.auth);

  const navItems: NavItem[] = [
    // The procurement lifecycle is the spine of the system, so it leads the nav.
    { label: 'Procurement', path: '/procurement', icon: <DashboardIcon /> },
    { label: 'Packages', path: '/procurement/packages', icon: <DocumentsIcon /> },
    { label: 'Expiries', path: '/procurement/expiries', icon: <ExpiryIcon /> },
    { label: 'Needs Attention', path: '/procurement/exceptions', icon: <ArchiveIcon /> },
    { label: 'Search', path: '/search', icon: <SearchIcon /> },
    { label: 'Notifications', path: '/notifications', icon: <NotificationsIcon /> },
    { label: 'Reports', path: '/reports', icon: <AppIcon /> },
    { label: 'Assets', path: '/assets', icon: <AssetsIcon /> },
    { label: 'Assignments', path: '/asset-assignments', icon: <AssignmentsIcon /> },
    { label: 'Workflows', path: '/workflows', icon: <WorkflowsIcon /> },
    { label: 'Health', path: '/health', icon: <HealthIcon /> },
    { label: 'Stationery', path: '/stationery', icon: <StationeryIcon /> },
    { label: 'Users', path: '/users', icon: <UsersIcon />, role: 'ADMIN' },
    { label: 'Field Catalogue', path: '/document-type-fields', icon: <FieldsIcon />, role: 'ADMIN' },

    // The document-centric pages the procurement workspace supersedes. They are removed
    // from the navigation in Phase 8, after pilot sign-off (R-8) — keeping them here means
    // a user blocked by a stage gate still has somewhere to go. Bill Entries is not
    // retired at all: the finance module stays independent (Q-6).
    { section: 'Documents', label: 'All Documents', path: '/documents', icon: <DocumentsIcon /> },
    { label: 'Expiry Tracking', path: '/expiry-tracking', icon: <ExpiryIcon /> },
    { label: 'Versions', path: '/versioning', icon: <VersioningIcon /> },
    { label: 'Archive', path: '/archive', icon: <ArchiveIcon /> },
    { label: 'APP Entries', path: '/app-entries', icon: <AppIcon /> },
    { label: 'Bill Entries', path: '/bill-entries', icon: <BillIcon /> },
    { label: 'Document Dashboard', path: '/dashboard', icon: <ReportsIcon /> },
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
      {/* Logo & Notification Bell */}
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
          onClick={() => navigate('/procurement')}
        >
          Document Manager
        </Typography>
        <NotificationBell />
      </Box>

      {/* Navigation Items */}
      <List sx={{ px: 1.5, py: 1.5, flex: 1 }}>
        {filteredNavItems.map((item) => (
          <React.Fragment key={item.path}>
          {item.section && (
            <Typography
              variant="caption"
              sx={{
                display: 'block',
                px: 1.5,
                pt: 2,
                pb: 0.5,
                color: '#9ca3af',
                fontSize: '0.7rem',
                fontWeight: 600,
                letterSpacing: '0.06em',
                textTransform: 'uppercase',
              }}
            >
              {item.section}
            </Typography>
          )}
          <ListItem disablePadding sx={{ mb: 0.5 }}>
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
          </React.Fragment>
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
