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
  Tabs,
  Tab,
  Alert,
  CircularProgress,
  Tooltip,
  Checkbox,
} from '@mui/material';
import {
  Unarchive as UnarchiveIcon,
  Restore as RestoreIcon,
  Delete as DeleteIcon,
  Download as DownloadIcon,
  Visibility as ViewIcon,
} from '@mui/icons-material';
import { documentService, Document } from '../services/documentService';
import { getDocumentTypeLabel, getDocumentTypeColor } from '../constants/documentTypes';

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

const Archive: React.FC = () => {
  const [tabValue, setTabValue] = useState(0);
  const [archivedDocuments, setArchivedDocuments] = useState<Document[]>([]);
  const [deletedDocuments, setDeletedDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedArchived, setSelectedArchived] = useState<number[]>([]);
  const [selectedDeleted, setSelectedDeleted] = useState<number[]>([]);
  const [statistics, setStatistics] = useState<{
    archivedCount: number;
    deletedCount: number;
    activeCount: number;
  } | null>(null);

  useEffect(() => {
    loadData();
  }, [tabValue]);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);

      if (tabValue === 0) {
        const response = await documentService.getArchivedDocuments({ page: 0, size: 1000 });
        setArchivedDocuments(response.content || []);
      } else {
        const response = await documentService.getDeletedDocuments({ page: 0, size: 1000 });
        setDeletedDocuments(response.content || []);
      }

      const stats = await documentService.getArchiveStatistics();
      setStatistics(stats);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load archive data');
    } finally {
      setLoading(false);
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

  const handleRestoreArchived = async (id: number) => {
    try {
      await documentService.restoreArchivedDocument(id);
      await loadData();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to restore document');
    }
  };

  const handleRestoreDeleted = async (id: number) => {
    try {
      await documentService.restoreDeletedDocument(id);
      await loadData();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to restore document');
    }
  };

  const handleBatchRestoreArchived = async () => {
    if (selectedArchived.length === 0) return;
    try {
      await documentService.restoreArchivedDocuments(selectedArchived);
      setSelectedArchived([]);
      await loadData();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to restore documents');
    }
  };

  const handleBatchRestoreDeleted = async () => {
    if (selectedDeleted.length === 0) return;
    try {
      await documentService.restoreDeletedDocuments(selectedDeleted);
      setSelectedDeleted([]);
      await loadData();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to restore documents');
    }
  };

  const handleSelectArchived = (id: number) => {
    setSelectedArchived((prev) =>
      prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id]
    );
  };

  const handleSelectDeleted = (id: number) => {
    setSelectedDeleted((prev) =>
      prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id]
    );
  };

  const handleSelectAllArchived = () => {
    if (selectedArchived.length === archivedDocuments.length) {
      setSelectedArchived([]);
    } else {
      setSelectedArchived(archivedDocuments.map((d) => d.id!).filter((id): id is number => id !== undefined));
    }
  };

  const handleSelectAllDeleted = () => {
    if (selectedDeleted.length === deletedDocuments.length) {
      setSelectedDeleted([]);
    } else {
      setSelectedDeleted(deletedDocuments.map((d) => d.id!).filter((id): id is number => id !== undefined));
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
          Archive Management
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
          <Grid item xs={12} md={4}>
            <Card>
              <CardContent>
                <Typography color="textSecondary" gutterBottom>
                  Active Documents
                </Typography>
                <Typography variant="h4">{statistics.activeCount}</Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={4}>
            <Card>
              <CardContent>
                <Typography color="textSecondary" gutterBottom>
                  Archived Documents
                </Typography>
                <Typography variant="h4">{statistics.archivedCount}</Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={4}>
            <Card>
              <CardContent>
                <Typography color="textSecondary" gutterBottom>
                  Deleted Documents
                </Typography>
                <Typography variant="h4">{statistics.deletedCount}</Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* Tabs */}
      <Card>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={(_, newValue) => setTabValue(newValue)}>
            <Tab label={`Archived (${archivedDocuments.length})`} />
            <Tab label={`Deleted (${deletedDocuments.length})`} />
          </Tabs>
        </Box>

        <CardContent>
          <TabPanel value={tabValue} index={0}>
            {selectedArchived.length > 0 && (
              <Box sx={{ mb: 2 }}>
                <Button
                  variant="contained"
                  startIcon={<UnarchiveIcon />}
                  onClick={handleBatchRestoreArchived}
                >
                  Restore Selected ({selectedArchived.length})
                </Button>
              </Box>
            )}
            {loading ? (
              <Box display="flex" justifyContent="center" p={3}>
                <CircularProgress />
              </Box>
            ) : archivedDocuments.length === 0 ? (
              <Alert severity="info">No archived documents found.</Alert>
            ) : (
              <TableContainer component={Paper}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell padding="checkbox">
                        <Checkbox
                          checked={selectedArchived.length === archivedDocuments.length && archivedDocuments.length > 0}
                          indeterminate={selectedArchived.length > 0 && selectedArchived.length < archivedDocuments.length}
                          onChange={handleSelectAllArchived}
                        />
                      </TableCell>
                      <TableCell>File Name</TableCell>
                      <TableCell>Type</TableCell>
                      <TableCell>Department</TableCell>
                      <TableCell>Archived Date</TableCell>
                      <TableCell>Size</TableCell>
                      <TableCell>Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {archivedDocuments.map((document) => (
                      <TableRow key={document.id}>
                        <TableCell padding="checkbox">
                          <Checkbox
                            checked={document.id ? selectedArchived.includes(document.id) : false}
                            onChange={() => document.id && handleSelectArchived(document.id)}
                          />
                        </TableCell>
                        <TableCell>
                          <Typography variant="subtitle2">
                            {document.originalName || document.fileName}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={getDocumentTypeLabel(document.documentType)}
                            color={getDocumentTypeColor(document.documentType)}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>{document.department || '-'}</TableCell>
                        <TableCell>
                          {document.archivedAt
                            ? new Date(document.archivedAt).toLocaleString()
                            : '-'}
                        </TableCell>
                        <TableCell>{formatFileSize(document.size ?? document.fileSize)}</TableCell>
                        <TableCell>
                          <Tooltip title="Restore">
                            <IconButton
                              size="small"
                              onClick={() => document.id && handleRestoreArchived(document.id)}
                              color="primary"
                            >
                              <UnarchiveIcon />
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </TabPanel>

          <TabPanel value={tabValue} index={1}>
            {selectedDeleted.length > 0 && (
              <Box sx={{ mb: 2 }}>
                <Button
                  variant="contained"
                  startIcon={<RestoreIcon />}
                  onClick={handleBatchRestoreDeleted}
                >
                  Restore Selected ({selectedDeleted.length})
                </Button>
              </Box>
            )}
            {loading ? (
              <Box display="flex" justifyContent="center" p={3}>
                <CircularProgress />
              </Box>
            ) : deletedDocuments.length === 0 ? (
              <Alert severity="info">No deleted documents found.</Alert>
            ) : (
              <TableContainer component={Paper}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell padding="checkbox">
                        <Checkbox
                          checked={selectedDeleted.length === deletedDocuments.length && deletedDocuments.length > 0}
                          indeterminate={selectedDeleted.length > 0 && selectedDeleted.length < deletedDocuments.length}
                          onChange={handleSelectAllDeleted}
                        />
                      </TableCell>
                      <TableCell>File Name</TableCell>
                      <TableCell>Type</TableCell>
                      <TableCell>Department</TableCell>
                      <TableCell>Deleted Date</TableCell>
                      <TableCell>Size</TableCell>
                      <TableCell>Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {deletedDocuments.map((document) => (
                      <TableRow key={document.id}>
                        <TableCell padding="checkbox">
                          <Checkbox
                            checked={document.id ? selectedDeleted.includes(document.id) : false}
                            onChange={() => document.id && handleSelectDeleted(document.id)}
                          />
                        </TableCell>
                        <TableCell>
                          <Typography variant="subtitle2">
                            {document.originalName || document.fileName}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={getDocumentTypeLabel(document.documentType)}
                            color={getDocumentTypeColor(document.documentType)}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>{document.department || '-'}</TableCell>
                        <TableCell>
                          {document.deletedAt
                            ? new Date(document.deletedAt).toLocaleString()
                            : '-'}
                        </TableCell>
                        <TableCell>{formatFileSize(document.size ?? document.fileSize)}</TableCell>
                        <TableCell>
                          <Tooltip title="Restore">
                            <IconButton
                              size="small"
                              onClick={() => document.id && handleRestoreDeleted(document.id)}
                              color="primary"
                            >
                              <RestoreIcon />
                            </IconButton>
                          </Tooltip>
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
    </Box>
  );
};

export default Archive;

