import React from 'react';
import {
  AppBar,
  Toolbar,
  Typography,
  Button,
  Box,
  IconButton,
  Menu,
  MenuItem,
} from '@mui/material';
import {
  AccountCircle,
  Logout,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../hooks/redux';
import { logout } from '../store/slices/authSlice';

const Navbar: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { user } = useAppSelector((state) => state.auth);
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);

  const handleMenu = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
  };

  const handleNavigation = (path: string) => {
    navigate(path);
    handleClose();
  };

  return (
    <AppBar position="static">
      <Toolbar>
        <Typography
          variant="h6"
          component="div"
          sx={{ flexGrow: 1, cursor: 'pointer' }}
          onClick={() => navigate('/dashboard')}
        >
          DMS
        </Typography>

        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button color="inherit" onClick={() => navigate('/dashboard')}>
            Dashboard
          </Button>
          <Button color="inherit" onClick={() => navigate('/documents')}>
            Documents
          </Button>
          <Button color="inherit" onClick={() => navigate('/search')}>
            Search
          </Button>
          <Button color="inherit" onClick={() => navigate('/notifications')}>
            Notifications
          </Button>
          <Button color="inherit" onClick={() => navigate('/expiry-tracking')}>
            Expiry Tracking
          </Button>
          <Button color="inherit" onClick={() => navigate('/reports')}>
            Reports
          </Button>
          <Button color="inherit" onClick={() => navigate('/analytics')}>
            Analytics
          </Button>
          <Button color="inherit" onClick={() => navigate('/workflows')}>
            Workflows
          </Button>
          <Button color="inherit" onClick={() => navigate('/versioning')}>
            Versioning
          </Button>
          <Button color="inherit" onClick={() => navigate('/integrations')}>
            Integrations
          </Button>
          <Button color="inherit" onClick={() => navigate('/analytics')}>
            Analytics
          </Button>
          <Button color="inherit" onClick={() => navigate('/ml')}>
            ML Models
          </Button>
          <Button color="inherit" onClick={() => navigate('/health')}>
            Health
          </Button>
          {user?.role === 'ADMIN' && (
            <Button color="inherit" onClick={() => navigate('/users')}>
              Users
            </Button>
          )}
        </Box>

        <Box sx={{ ml: 2 }}>
          <IconButton
            size="large"
            aria-label="account of current user"
            aria-controls="menu-appbar"
            aria-haspopup="true"
            onClick={handleMenu}
            color="inherit"
          >
            <AccountCircle />
          </IconButton>
          <Menu
            id="menu-appbar"
            anchorEl={anchorEl}
            anchorOrigin={{
              vertical: 'top',
              horizontal: 'right',
            }}
            keepMounted
            transformOrigin={{
              vertical: 'top',
              horizontal: 'right',
            }}
            open={Boolean(anchorEl)}
            onClose={handleClose}
          >
            <MenuItem onClick={() => handleNavigation('/dashboard')}>
              Dashboard
            </MenuItem>
            <MenuItem onClick={() => handleNavigation('/documents')}>
              Documents
            </MenuItem>
            <MenuItem onClick={() => handleNavigation('/search')}>
              Search
            </MenuItem>
            <MenuItem onClick={() => handleNavigation('/notifications')}>
              Notifications
            </MenuItem>
            <MenuItem onClick={() => handleNavigation('/expiry-tracking')}>
              Expiry Tracking
            </MenuItem>
            <MenuItem onClick={() => handleNavigation('/reports')}>
              Reports
            </MenuItem>
            <MenuItem onClick={() => handleNavigation('/analytics')}>
              Analytics
            </MenuItem>
            <MenuItem onClick={() => handleNavigation('/workflows')}>
              Workflows
            </MenuItem>
            <MenuItem onClick={() => handleNavigation('/versioning')}>
              Versioning
            </MenuItem>
            <MenuItem onClick={() => handleNavigation('/integrations')}>
              Integrations
            </MenuItem>
            <MenuItem onClick={() => handleNavigation('/analytics')}>
              Analytics
            </MenuItem>
            <MenuItem onClick={() => handleNavigation('/ml')}>
              ML Models
            </MenuItem>
            <MenuItem onClick={() => handleNavigation('/health')}>
              Health
            </MenuItem>
            {user?.role === 'ADMIN' && (
              <MenuItem onClick={() => handleNavigation('/users')}>
                Users
              </MenuItem>
            )}
            <MenuItem onClick={handleLogout}>
              <Logout sx={{ mr: 1 }} />
              Logout
            </MenuItem>
          </Menu>
        </Box>
      </Toolbar>
    </AppBar>
  );
};

export default Navbar;
