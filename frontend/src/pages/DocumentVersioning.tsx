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
  Tooltip,
  Divider,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction
} from '@mui/material';
import {
  RestoreFromTrash,
  Visibility,
  Download,
  Compare,
  Add,
  Archive,
  History,
  Description
} from '@mui/icons-material';
import { documentVersioningService, DocumentVersion } from '../services/documentVersioningService';

interface DocumentVersioningProps {
  documentId?: number;
}

const DocumentVersioning: React.FC<DocumentVersioningProps> = ({ documentId }) => {
  const [versions, setVersions] = useState<DocumentVersion[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedVersions, setSelectedVersions] = useState<string[]>([]);
  const [restoreDialogOpen, setRestoreDialogOpen] = useState(false);
  const [compareDialogOpen, setCompareDialogOpen] = useState(false);
  const [versionToRestore, setVersionToRestore] = useState<string>('');
  const [comparison, setComparison] = useState<any>(null);
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const [newVersionFile, setNewVersionFile] = useState<File | null>(null);
  const [changeDescription, setChangeDescription] = useState('');
  const [versionType, setVersionType] = useState('MINOR');

  useEffect(() => {
    if (documentId) {
      loadVersions();
    }
  }, [documentId]);

  const loadVersions = async () => {
    if (!documentId) return;

    try {
      setLoading(true);
      const versionsData = await documentVersioningService.getDocumentVersions(documentId);
      setVersions(versionsData);
    } catch (err) {
      setError('Failed to load document versions');
    } finally {
      setLoading(false);
    }
  };

  const handleVersionRestore = async () => {
    if (!documentId || !versionToRestore) return;

    try {
      await documentVersioningService.restoreDocumentToVersion(documentId, versionToRestore);
      setRestoreDialogOpen(false);
      loadVersions();
    } catch (err) {
      setError('Failed to restore document version');
    }
  };

  const handleVersionCompare = async () => {
    if (selectedVersions.length !== 2) return;

    try {
      const comparisonData = await documentVersioningService.compareVersions(
        documentId!,
        selectedVersions[0],
        selectedVersions[1]
      );
      setComparison(comparisonData);
      setCompareDialogOpen(true);
    } catch (err) {
      setError('Failed to compare versions');
    }
  };

  const handleVersionUpload = async () => {
    if (!documentId || !newVersionFile) return;

    try {
      await documentVersioningService.createDocumentVersion(
        documentId,
        newVersionFile,
        changeDescription,
        versionType
      );
      setUploadDialogOpen(false);
      setNewVersionFile(null);
      setChangeDescription('');
      setVersionType('MINOR');
      loadVersions();
    } catch (err) {
      setError('Failed to create new version');
    }
  };

  const handleVersionDownload = async (versionNumber: string) => {
    if (!documentId) return;

    try {
      const blob = await documentVersioningService.downloadVersion(documentId, versionNumber);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `version_${versionNumber}`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      setError('Failed to download version');
    }
  };

  const getVersionTypeColor = (type: string) => {
    switch (type) {
      case 'MAJOR': return 'error';
      case 'MINOR': return 'warning';
      case 'PATCH': return 'info';
      case 'DRAFT': return 'default';
      case 'FINAL': return 'success';
      default: return 'default';
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" component="h1">
          Document Versioning
        </Typography>
        <Box>
          <Button
            variant="outlined"
            startIcon={<Add />}
            onClick={() => setUploadDialogOpen(true)}
            sx={{ mr: 1 }}
          >
            New Version
          </Button>
          <Button
            variant="outlined"
            startIcon={<Compare />}
            onClick={handleVersionCompare}
            disabled={selectedVersions.length !== 2}
          >
            Compare
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Version Statistics */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Total Versions
              </Typography>
              <Typography variant="h4">
                {versions.length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Current Version
              </Typography>
              <Typography variant="h4">
                {versions.find(v => v.isCurrent)?.versionNumber || 'N/A'}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Archived Versions
              </Typography>
              <Typography variant="h4">
                {versions.filter(v => v.isArchived).length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Total Size
              </Typography>
              <Typography variant="h4">
                {formatFileSize(versions.reduce((sum, v) => sum + (v.fileSize || 0), 0))}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Versions Table */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell padding="checkbox">
                <input
                  type="checkbox"
                  onChange={(e) => {
                    if (e.target.checked) {
                      setSelectedVersions(versions.map(v => v.versionNumber));
                    } else {
                      setSelectedVersions([]);
                    }
                  }}
                />
              </TableCell>
              <TableCell>Version</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Created By</TableCell>
              <TableCell>Change Description</TableCell>
              <TableCell>File Size</TableCell>
              <TableCell>Created At</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {versions.map((version) => (
              <TableRow key={version.id}>
                <TableCell padding="checkbox">
                  <input
                    type="checkbox"
                    checked={selectedVersions.includes(version.versionNumber)}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setSelectedVersions([...selectedVersions, version.versionNumber]);
                      } else {
                        setSelectedVersions(selectedVersions.filter(v => v !== version.versionNumber));
                      }
                    }}
                  />
                </TableCell>
                <TableCell>
                  <Typography variant="subtitle2">
                    {version.versionNumber}
                    {version.isCurrent && (
                      <Chip label="Current" color="primary" size="small" sx={{ ml: 1 }} />
                    )}
                  </Typography>
                </TableCell>
                <TableCell>
                  <Chip
                    label={version.versionType}
                    color={getVersionTypeColor(version.versionType) as any}
                    size="small"
                  />
                </TableCell>
                <TableCell>
                  {version.createdBy.username}
                </TableCell>
                <TableCell>
                  <Typography variant="body2" noWrap>
                    {version.changeDescription || 'No description'}
                  </Typography>
                </TableCell>
                <TableCell>
                  {formatFileSize(version.fileSize || 0)}
                </TableCell>
                <TableCell>
                  {new Date(version.createdAt).toLocaleDateString()}
                </TableCell>
                <TableCell>
                  <Chip
                    label={version.isArchived ? 'Archived' : 'Active'}
                    color={version.isArchived ? 'default' : 'success'}
                    size="small"
                  />
                </TableCell>
                <TableCell>
                  <Tooltip title="Download Version">
                    <IconButton
                      size="small"
                      onClick={() => handleVersionDownload(version.versionNumber)}
                    >
                      <Download />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title="Restore Version">
                    <IconButton
                      size="small"
                      onClick={() => {
                        setVersionToRestore(version.versionNumber);
                        setRestoreDialogOpen(true);
                      }}
                      disabled={version.isCurrent}
                    >
                      <RestoreFromTrash />
                    </IconButton>
                  </Tooltip>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Restore Dialog */}
      <Dialog open={restoreDialogOpen} onClose={() => setRestoreDialogOpen(false)}>
        <DialogTitle>Restore Document Version</DialogTitle>
        <DialogContent>
          <Typography>
            Are you sure you want to restore this document to version {versionToRestore}?
            This will create a new version with the current content as a backup.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRestoreDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleVersionRestore} variant="contained" color="warning">
            Restore
          </Button>
        </DialogActions>
      </Dialog>

      {/* Compare Dialog */}
      <Dialog open={compareDialogOpen} onClose={() => setCompareDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Version Comparison</DialogTitle>
        <DialogContent>
          {comparison && (
            <Grid container spacing={3}>
              <Grid item xs={6}>
                <Typography variant="h6" gutterBottom>
                  Version {comparison.version1.versionNumber}
                </Typography>
                <List>
                  <ListItem>
                    <ListItemText
                      primary="Created By"
                      secondary={comparison.version1.createdBy}
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText
                      primary="Created At"
                      secondary={new Date(comparison.version1.createdAt).toLocaleString()}
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText
                      primary="File Size"
                      secondary={formatFileSize(comparison.version1.fileSize)}
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText
                      primary="Description"
                      secondary={comparison.version1.changeDescription}
                    />
                  </ListItem>
                </List>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="h6" gutterBottom>
                  Version {comparison.version2.versionNumber}
                </Typography>
                <List>
                  <ListItem>
                    <ListItemText
                      primary="Created By"
                      secondary={comparison.version2.createdBy}
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText
                      primary="Created At"
                      secondary={new Date(comparison.version2.createdAt).toLocaleString()}
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText
                      primary="File Size"
                      secondary={formatFileSize(comparison.version2.fileSize)}
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText
                      primary="Description"
                      secondary={comparison.version2.changeDescription}
                    />
                  </ListItem>
                </List>
              </Grid>
              <Grid item xs={12}>
                <Divider sx={{ my: 2 }} />
                <Typography variant="h6" gutterBottom>
                  Differences
                </Typography>
                <List>
                  <ListItem>
                    <ListItemText
                      primary="File Size Changed"
                      secondary={comparison.differences.fileSizeChanged ? 'Yes' : 'No'}
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText
                      primary="Content Changed"
                      secondary={comparison.differences.contentChanged ? 'Yes' : 'No'}
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText
                      primary="Time Difference"
                      secondary={`${comparison.differences.timeDifference} days`}
                    />
                  </ListItem>
                </List>
              </Grid>
            </Grid>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCompareDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* Upload Dialog */}
      <Dialog open={uploadDialogOpen} onClose={() => setUploadDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create New Version</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Change Description"
            fullWidth
            multiline
            rows={3}
            variant="outlined"
            value={changeDescription}
            onChange={(e) => setChangeDescription(e.target.value)}
            sx={{ mb: 2 }}
          />
          <FormControl fullWidth sx={{ mb: 2 }}>
            <InputLabel>Version Type</InputLabel>
            <Select
              value={versionType}
              onChange={(e) => setVersionType(e.target.value)}
            >
              <MenuItem value="MAJOR">Major</MenuItem>
              <MenuItem value="MINOR">Minor</MenuItem>
              <MenuItem value="PATCH">Patch</MenuItem>
              <MenuItem value="DRAFT">Draft</MenuItem>
              <MenuItem value="FINAL">Final</MenuItem>
            </Select>
          </FormControl>
          <Button
            variant="outlined"
            component="label"
            fullWidth
            startIcon={<Add />}
          >
            Select File
            <input
              type="file"
              hidden
              onChange={(e) => setNewVersionFile(e.target.files?.[0] || null)}
            />
          </Button>
          {newVersionFile && (
            <Typography variant="body2" sx={{ mt: 1 }}>
              Selected: {newVersionFile.name}
            </Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setUploadDialogOpen(false)}>Cancel</Button>
          <Button
            onClick={handleVersionUpload}
            variant="contained"
            disabled={!newVersionFile || !changeDescription}
          >
            Create Version
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default DocumentVersioning;
