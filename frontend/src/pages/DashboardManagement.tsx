import React, { useState, useEffect } from 'react';
import {
  Box,
  Grid,
  Typography,
  Card,
  CardContent,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
  CircularProgress,
  Alert,
  IconButton,
  Chip,
} from '@mui/material';
import {
  Add as AddIcon,
  Dashboard as DashboardIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Visibility as ViewIcon,
  Public as PublicIcon,
  Lock as PrivateIcon,
} from '@mui/icons-material';
import axios from 'axios';
import { API_BASE_URL } from '../constants';

interface Dashboard {
  id: number;
  name: string;
  description: string;
  dashboardType: string;
  isPublic: boolean;
  isDefault: boolean;
  accessCount: number;
  createdAt: string;
}

const DashboardManagement: React.FC = () => {
  const [dashboards, setDashboards] = useState<Dashboard[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [openDialog, setOpenDialog] = useState(false);
  const [selectedDashboard, setSelectedDashboard] = useState<Dashboard | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    dashboardType: 'EXECUTIVE',
  });

  useEffect(() => {
    fetchDashboards();
  }, []);

  const fetchDashboards = async () => {
    setLoading(true);
    setError('');
    
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(`${API_BASE_URL}/dashboards`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setDashboards(response.data.content || []);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch dashboards');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async () => {
    try {
      const token = localStorage.getItem('token');
      await axios.post(`${API_BASE_URL}/dashboards`, formData, {
        headers: { Authorization: `Bearer ${token}` },
      });
      
      setOpenDialog(false);
      setFormData({ name: '', description: '', dashboardType: 'EXECUTIVE' });
      fetchDashboards();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create dashboard');
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this dashboard?')) return;
    
    try {
      const token = localStorage.getItem('token');
      await axios.delete(`${API_BASE_URL}/dashboards/${id}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      fetchDashboards();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete dashboard');
    }
  };

  const dashboardTypes = [
    { value: 'EXECUTIVE', label: 'Executive' },
    { value: 'OPERATIONAL', label: 'Operational' },
    { value: 'ANALYTICAL', label: 'Analytical' },
    { value: 'CUSTOM', label: 'Custom' },
  ];

  return (
    <Box sx={{ p: 4 }}>
      {/* Header */}
      <Box sx={{ mb: 4, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Box>
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
            Dashboard Management
          </Typography>
          <Typography variant="body2" sx={{ color: '#6b7280' }}>
            Create and manage custom dashboards
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setOpenDialog(true)}
          sx={{
            backgroundColor: '#3b82f6',
            textTransform: 'none',
            fontWeight: 600,
            boxShadow: 'none',
            '&:hover': {
              backgroundColor: '#2563eb',
            },
          }}
        >
          Create Dashboard
        </Button>
      </Box>

      {/* Error Alert */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      {/* Dashboards Grid */}
      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Grid container spacing={3}>
          {dashboards.map((dashboard) => (
            <Grid item xs={12} sm={6} md={4} key={dashboard.id}>
              <Card
                sx={{
                  borderRadius: 3,
                  boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
                  border: '1px solid #f3f4f6',
                }}
              >
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'start', justifyContent: 'space-between', mb: 2 }}>
                    <Box
                      sx={{
                        width: 40,
                        height: 40,
                        borderRadius: 2,
                        backgroundColor: '#eff6ff',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: '#3b82f6',
                      }}
                    >
                      <DashboardIcon />
                    </Box>
                    <Box>
                      {dashboard.isPublic ? (
                        <Chip icon={<PublicIcon />} label="Public" size="small" color="success" />
                      ) : (
                        <Chip icon={<PrivateIcon />} label="Private" size="small" />
                      )}
                    </Box>
                  </Box>

                  <Typography variant="h6" sx={{ fontWeight: 600, mb: 1 }}>
                    {dashboard.name}
                  </Typography>

                  <Typography variant="body2" sx={{ color: '#6b7280', mb: 2 }}>
                    {dashboard.description || 'No description'}
                  </Typography>

                  <Typography variant="caption" sx={{ color: '#9ca3af' }}>
                    Type: {dashboard.dashboardType} • Views: {dashboard.accessCount}
                  </Typography>

                  <Box sx={{ mt: 2, display: 'flex', gap: 1 }}>
                    <IconButton size="small" color="primary">
                      <ViewIcon fontSize="small" />
                    </IconButton>
                    <IconButton size="small" color="default">
                      <EditIcon fontSize="small" />
                    </IconButton>
                    <IconButton size="small" color="error" onClick={() => handleDelete(dashboard.id)}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          ))}

          {dashboards.length === 0 && (
            <Grid item xs={12}>
              <Box sx={{ textAlign: 'center', py: 8, color: '#9ca3af' }}>
                <DashboardIcon sx={{ fontSize: 64, mb: 2 }} />
                <Typography>No dashboards yet. Create your first one!</Typography>
              </Box>
            </Grid>
          )}
        </Grid>
      )}

      {/* Create Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create New Dashboard</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 3 }}>
            <TextField
              fullWidth
              label="Dashboard Name"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            />
            <TextField
              fullWidth
              label="Description"
              multiline
              rows={3}
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            />
            <FormControl fullWidth>
              <InputLabel>Dashboard Type</InputLabel>
              <Select
                value={formData.dashboardType}
                label="Dashboard Type"
                onChange={(e) => setFormData({ ...formData, dashboardType: e.target.value })}
              >
                {dashboardTypes.map((type) => (
                  <MenuItem key={type.value} value={type.value}>
                    {type.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={!formData.name}>
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default DashboardManagement;

