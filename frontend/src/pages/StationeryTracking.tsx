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
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
  CircularProgress,
  Tooltip,
  Tabs,
  Tab,
} from '@mui/material';
import {
  Person as PersonIcon,
  Assignment as AssignmentIcon,
  PersonAdd as PersonAddIcon,
  PersonRemove as PersonRemoveIcon,
  Assessment as ReportsIcon,
} from '@mui/icons-material';
import { documentService, Document } from '../services/documentService';
import { userManagementService } from '../services/userManagementService';
import { UserDetail } from '../types/userManagement';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div role="tabpanel" hidden={value !== index} {...other}>
      {value === index && <Box sx={{ pt: 3 }}>{children}</Box>}
    </div>
  );
}

const StationeryTracking: React.FC = () => {
  const [tabValue, setTabValue] = useState(0);
  const [stationeryRecords, setStationeryRecords] = useState<Document[]>([]);
  const [employees, setEmployees] = useState<UserDetail[]>([]);
  const [employeeStats, setEmployeeStats] = useState<Array<{
    employeeId: number;
    username: string;
    firstName?: string;
    lastName?: string;
    email?: string;
    stationeryCount: number;
    stationeryRecords: Document[];
  }>>([]);
  const [statistics, setStatistics] = useState<{
    totalStationeryRecords: number;
    assignedRecords: number;
    unassignedRecords: number;
    employeesWithStationery: number;
  } | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [assignDialogOpen, setAssignDialogOpen] = useState(false);
  const [selectedDocument, setSelectedDocument] = useState<Document | null>(null);
  const [selectedEmployeeId, setSelectedEmployeeId] = useState<number | ''>('');

  useEffect(() => {
    loadData();
    loadEmployees();
  }, [tabValue]);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);

      if (tabValue === 0) {
        const response = await documentService.getStationeryRecords({ page: 0, size: 1000 });
        setStationeryRecords(response.content || []);
      } else if (tabValue === 1) {
        const stats = await documentService.getStationeryStatisticsPerEmployee();
        setEmployeeStats(stats);
      }

      const stats = await documentService.getStationeryStatistics();
      setStatistics(stats);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load stationery data');
    } finally {
      setLoading(false);
    }
  };

  const loadEmployees = async () => {
    try {
      // Load all users for assignment
      const response = await userManagementService.getUsers({ size: 1000 });
      setEmployees(response.content || []);
    } catch (err) {
      console.error('Failed to load employees:', err);
    }
  };

  const formatFileSize = (value?: string | number): string => {
    if (value === undefined || value === null) return '-';
    const bytes = typeof value === 'string' ? parseInt(value, 10) : value;
    if (!Number.isFinite(bytes) || bytes <= 0) {
      return typeof value === 'string' ? value : `${bytes}`;
    }
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const handleAssignClick = (document: Document) => {
    setSelectedDocument(document);
    setSelectedEmployeeId(document.assignedEmployee?.id || '');
    setAssignDialogOpen(true);
  };

  const handleAssign = async () => {
    if (!selectedDocument || !selectedDocument.id || !selectedEmployeeId) {
      return;
    }

    try {
      await documentService.assignStationeryToEmployee(selectedDocument.id, selectedEmployeeId as number);
      setAssignDialogOpen(false);
      await loadData();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to assign stationery');
    }
  };

  const handleUnassign = async (documentId: number) => {
    if (!window.confirm('Are you sure you want to unassign this stationery record?')) {
      return;
    }

    try {
      await documentService.unassignStationeryFromEmployee(documentId);
      await loadData();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to unassign stationery');
    }
  };

  if (loading && !statistics) {
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
          Stationery Tracking
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Statistics Cards */}
      {statistics && (
        <Grid container spacing={3} sx={{ mb: 3 }}>
          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Typography color="textSecondary" gutterBottom>
                  Total Stationery Records
                </Typography>
                <Typography variant="h4">{statistics.totalStationeryRecords}</Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Typography color="textSecondary" gutterBottom>
                  Assigned Records
                </Typography>
                <Typography variant="h4">{statistics.assignedRecords}</Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Typography color="textSecondary" gutterBottom>
                  Unassigned Records
                </Typography>
                <Typography variant="h4">{statistics.unassignedRecords}</Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Typography color="textSecondary" gutterBottom>
                  Employees with Stationery
                </Typography>
                <Typography variant="h4">{statistics.employeesWithStationery}</Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* Tabs */}
      <Card>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={(_, newValue) => setTabValue(newValue)}>
            <Tab label={`All Stationery Records (${stationeryRecords.length})`} />
            <Tab label={`By Employee (${employeeStats.length})`} />
          </Tabs>
        </Box>

        <CardContent>
          <TabPanel value={tabValue} index={0}>
            {loading ? (
              <Box display="flex" justifyContent="center" p={3}>
                <CircularProgress />
              </Box>
            ) : stationeryRecords.length === 0 ? (
              <Alert severity="info">No stationery records found.</Alert>
            ) : (
              <TableContainer component={Paper}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>File Name</TableCell>
                      <TableCell>Description</TableCell>
                      <TableCell>Assigned Employee</TableCell>
                      <TableCell>Size</TableCell>
                      <TableCell>Created Date</TableCell>
                      <TableCell>Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {stationeryRecords.map((document) => (
                      <TableRow key={document.id}>
                        <TableCell>
                          <Typography variant="subtitle2">
                            {document.originalName || document.fileName}
                          </Typography>
                        </TableCell>
                        <TableCell>{document.description || '-'}</TableCell>
                        <TableCell>
                          {document.assignedEmployee ? (
                            <Chip
                              label={document.assignedEmployee.username}
                              icon={<PersonIcon />}
                              size="small"
                              color="primary"
                            />
                          ) : (
                            <Chip label="Unassigned" size="small" color="default" />
                          )}
                        </TableCell>
                        <TableCell>{formatFileSize(document.size ?? document.fileSize)}</TableCell>
                        <TableCell>
                          {document.createdAt
                            ? new Date(document.createdAt).toLocaleDateString()
                            : '-'}
                        </TableCell>
                        <TableCell>
                          <Tooltip title={document.assignedEmployee ? "Reassign" : "Assign"}>
                            <IconButton
                              size="small"
                              onClick={() => handleAssignClick(document)}
                              color="primary"
                            >
                              <PersonAddIcon />
                            </IconButton>
                          </Tooltip>
                          {document.assignedEmployee && (
                            <Tooltip title="Unassign">
                              <IconButton
                                size="small"
                                onClick={() => document.id && handleUnassign(document.id)}
                                color="error"
                              >
                                <PersonRemoveIcon />
                              </IconButton>
                            </Tooltip>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </TabPanel>

          <TabPanel value={tabValue} index={1}>
            {loading ? (
              <Box display="flex" justifyContent="center" p={3}>
                <CircularProgress />
              </Box>
            ) : employeeStats.length === 0 ? (
              <Alert severity="info">No employees with stationery records found.</Alert>
            ) : (
              <TableContainer component={Paper}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Employee</TableCell>
                      <TableCell>Email</TableCell>
                      <TableCell>Stationery Count</TableCell>
                      <TableCell>Records</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {employeeStats.map((stat) => (
                      <TableRow key={stat.employeeId}>
                        <TableCell>
                          <Typography variant="subtitle2">
                            {stat.firstName && stat.lastName
                              ? `${stat.firstName} ${stat.lastName}`
                              : stat.username}
                          </Typography>
                          <Typography variant="caption" color="textSecondary">
                            @{stat.username}
                          </Typography>
                        </TableCell>
                        <TableCell>{stat.email || '-'}</TableCell>
                        <TableCell>
                          <Chip label={stat.stationeryCount} color="primary" size="small" />
                        </TableCell>
                        <TableCell>
                          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                            {stat.stationeryRecords.slice(0, 3).map((record) => (
                              <Chip
                                key={record.id}
                                label={record.originalName || record.fileName}
                                size="small"
                                variant="outlined"
                              />
                            ))}
                            {stat.stationeryRecords.length > 3 && (
                              <Chip
                                label={`+${stat.stationeryRecords.length - 3} more`}
                                size="small"
                                variant="outlined"
                              />
                            )}
                          </Box>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </TabPanel>
        </CardContent>
      </Card>

      {/* Assign Dialog */}
      <Dialog open={assignDialogOpen} onClose={() => setAssignDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Assign Stationery Record</DialogTitle>
        <DialogContent>
          <FormControl fullWidth sx={{ mt: 2 }}>
            <InputLabel>Employee</InputLabel>
            <Select
              value={selectedEmployeeId}
              onChange={(e) => setSelectedEmployeeId(e.target.value as number)}
              label="Employee"
            >
              <MenuItem value="">Unassign</MenuItem>
              {employees.map((employee) => (
                <MenuItem key={employee.id} value={employee.id}>
                  {(employee.firstName || employee.lastName)
                    ? `${employee.firstName || ''} ${employee.lastName || ''}`.trim() + ` (${employee.username})`
                    : employee.username}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAssignDialogOpen(false)}>Cancel</Button>
          <Button
            onClick={handleAssign}
            variant="contained"
            disabled={!selectedEmployeeId}
          >
            Assign
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default StationeryTracking;

