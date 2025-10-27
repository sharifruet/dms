import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
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
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Grid,
  Alert,
  CircularProgress,
  Tabs,
  Tab,
  Pagination,
  Tooltip,
  Menu,
  ListItemIcon,
  ListItemText,
  FormControlLabel,
  Switch
} from '@mui/material';
import {
  Add as AddIcon,
  Download as DownloadIcon,
  MoreVert as MoreVertIcon,
  Description as DescriptionIcon,
  TableChart as TableChartIcon,
  PictureAsPdf as PictureAsPdfIcon,
  Description as WordIcon,
  FileDownload as FileDownloadIcon,
  Schedule as ScheduleIcon,
  Public as PublicIcon,
  Visibility as VisibilityIcon,
  Delete as DeleteIcon,
  Refresh as RefreshIcon
} from '@mui/icons-material';
import reportingService, { 
  Report, 
  ReportType, 
  ReportFormat, 
  ReportStatus, 
  CreateReportRequest 
} from '../services/reportingService';

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
      id={`report-tabpanel-${index}`}
      aria-labelledby={`report-tab-${index}`}
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

const Reports: React.FC = () => {
  const [reports, setReports] = useState<Report[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [tabValue, setTabValue] = useState(0);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [selectedReport, setSelectedReport] = useState<Report | null>(null);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  
  const [newReport, setNewReport] = useState<CreateReportRequest>({
    name: '',
    description: '',
    type: ReportType.DOCUMENT_SUMMARY,
    format: ReportFormat.PDF,
    parameters: {}
  });

  useEffect(() => {
    loadReports();
  }, [currentPage, tabValue]);

  const loadReports = async () => {
    try {
      setLoading(true);
      let response;
      
      if (tabValue === 0) {
        response = await reportingService.getReports(currentPage, 20);
      } else {
        response = await reportingService.getPublicReports(currentPage, 20);
      }
      
      setReports(response.content || []);
      setTotalPages(response.totalPages || 0);
    } catch (error) {
      setError('Failed to load reports');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateReport = async () => {
    try {
      await reportingService.createReport(newReport);
      setCreateDialogOpen(false);
      setNewReport({
        name: '',
        description: '',
        type: ReportType.DOCUMENT_SUMMARY,
        format: ReportFormat.PDF,
        parameters: {}
      });
      await loadReports();
    } catch (error) {
      setError('Failed to create report');
    }
  };

  const getStatusColor = (status: ReportStatus) => {
    switch (status) {
      case ReportStatus.COMPLETED:
        return 'success';
      case ReportStatus.FAILED:
        return 'error';
      case ReportStatus.GENERATING:
        return 'warning';
      case ReportStatus.PENDING:
        return 'info';
      case ReportStatus.EXPIRED:
        return 'default';
      case ReportStatus.CANCELLED:
        return 'default';
      default:
        return 'default';
    }
  };

  const getFormatIcon = (format: ReportFormat) => {
    switch (format) {
      case ReportFormat.PDF:
        return <PictureAsPdfIcon />;
      case ReportFormat.EXCEL:
        return <TableChartIcon />;
      case ReportFormat.WORD:
        return <WordIcon />;
      case ReportFormat.CSV:
        return <TableChartIcon />;
      case ReportFormat.JSON:
        return <DescriptionIcon />;
      case ReportFormat.HTML:
        return <DescriptionIcon />;
      default:
        return <DescriptionIcon />;
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  const formatFileSize = (bytes?: number) => {
    if (!bytes) return 'N/A';
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return Math.round(bytes / Math.pow(1024, i) * 100) / 100 + ' ' + sizes[i];
  };

  const filteredReports = reports.filter(report => {
    switch (tabValue) {
      case 0: // My Reports
        return true;
      case 1: // Public Reports
        return report.isPublic;
      case 2: // Completed Reports
        return report.status === ReportStatus.COMPLETED;
      case 3: // Scheduled Reports
        return report.isScheduled;
      default:
        return true;
    }
  });

  if (loading && reports.length === 0) {
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
          Reports
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setCreateDialogOpen(true)}
        >
          Create Report
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)}>
            <Tab label="My Reports" />
            <Tab label="Public Reports" />
            <Tab label="Completed" />
            <Tab label="Scheduled" />
          </Tabs>
        </Box>

        <TabPanel value={tabValue} index={0}>
          <Typography variant="h6" mb={2}>
            My Reports ({filteredReports.length})
          </Typography>
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Format</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Created</TableCell>
                  <TableCell>Generated</TableCell>
                  <TableCell>File Size</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filteredReports.map((report) => (
                  <TableRow key={report.id}>
                    <TableCell>
                      <Box display="flex" alignItems="center">
                        <DescriptionIcon sx={{ mr: 1 }} />
                        {report.name}
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Chip label={report.type.replace(/_/g, ' ')} size="small" />
                    </TableCell>
                    <TableCell>
                      <Tooltip title={report.format}>
                        {getFormatIcon(report.format)}
                      </Tooltip>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={report.status}
                        color={getStatusColor(report.status)}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>{formatDate(report.createdAt)}</TableCell>
                    <TableCell>
                      {report.generatedAt ? formatDate(report.generatedAt) : 'Not generated'}
                    </TableCell>
                    <TableCell>{formatFileSize(report.fileSize)}</TableCell>
                    <TableCell>
                      <IconButton
                        onClick={(e) => {
                          setAnchorEl(e.currentTarget);
                          setSelectedReport(report);
                        }}
                      >
                        <MoreVertIcon />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>

          {totalPages > 1 && (
            <Box display="flex" justifyContent="center" mt={3}>
              <Pagination
                count={totalPages}
                page={currentPage + 1}
                onChange={(e, page) => setCurrentPage(page - 1)}
              />
            </Box>
          )}
        </TabPanel>

        <TabPanel value={tabValue} index={1}>
          <Typography variant="h6" mb={2}>
            Public Reports ({filteredReports.length})
          </Typography>
          {/* Similar table structure for public reports */}
        </TabPanel>

        <TabPanel value={tabValue} index={2}>
          <Typography variant="h6" mb={2}>
            Completed Reports ({filteredReports.length})
          </Typography>
          {/* Similar table structure for completed reports */}
        </TabPanel>

        <TabPanel value={tabValue} index={3}>
          <Typography variant="h6" mb={2}>
            Scheduled Reports ({filteredReports.length})
          </Typography>
          {/* Similar table structure for scheduled reports */}
        </TabPanel>
      </Card>

      {/* Report Actions Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={() => setAnchorEl(null)}
      >
        {selectedReport && selectedReport.status === ReportStatus.COMPLETED && (
          <MenuItem onClick={() => setAnchorEl(null)}>
            <ListItemIcon>
              <DownloadIcon />
            </ListItemIcon>
            <ListItemText>Download</ListItemText>
          </MenuItem>
        )}
        <MenuItem onClick={() => setAnchorEl(null)}>
          <ListItemIcon>
            <VisibilityIcon />
          </ListItemIcon>
          <ListItemText>View Details</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => setAnchorEl(null)}>
          <ListItemIcon>
            <RefreshIcon />
          </ListItemIcon>
          <ListItemText>Regenerate</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => setAnchorEl(null)}>
          <ListItemIcon>
            <DeleteIcon />
          </ListItemIcon>
          <ListItemText>Delete</ListItemText>
        </MenuItem>
      </Menu>

      {/* Create Report Dialog */}
      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Create New Report</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Report Name"
                value={newReport.name}
                onChange={(e) => setNewReport({ ...newReport, name: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                rows={3}
                label="Description"
                value={newReport.description}
                onChange={(e) => setNewReport({ ...newReport, description: e.target.value })}
              />
            </Grid>
            <Grid item xs={6}>
              <FormControl fullWidth>
                <InputLabel>Report Type</InputLabel>
                <Select
                  value={newReport.type}
                  onChange={(e) => setNewReport({ ...newReport, type: e.target.value as ReportType })}
                >
                  {Object.values(ReportType).map((type) => (
                    <MenuItem key={type} value={type}>
                      {type.replace(/_/g, ' ')}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={6}>
              <FormControl fullWidth>
                <InputLabel>Output Format</InputLabel>
                <Select
                  value={newReport.format}
                  onChange={(e) => setNewReport({ ...newReport, format: e.target.value as ReportFormat })}
                >
                  {Object.values(ReportFormat).map((format) => (
                    <MenuItem key={format} value={format}>
                      {format}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <FormControlLabel
                control={<Switch />}
                label="Make this report public"
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleCreateReport} variant="contained">
            Create Report
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Reports;
