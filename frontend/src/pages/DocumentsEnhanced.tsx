import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
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
  Alert,
  CircularProgress,
  Tooltip,
  LinearProgress,
  Card,
  CardContent,
  Grid,
  InputAdornment,
  Menu,
  ListItemIcon,
  ListItemText,
  Pagination,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Download as DownloadIcon,
  Visibility as ViewIcon,
  Upload as UploadIcon,
  Search as SearchIcon,
  FilterList as FilterIcon,
  MoreVert as MoreVertIcon,
  InsertDriveFile as FileIcon,
  PictureAsPdf as PdfIcon,
  Image as ImageIcon,
  Description as DocIcon,
  TableChart as ExcelIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material';
import { useAppSelector } from '../hooks/redux';
import { documentService, Document, DocumentUploadRequest } from '../services/documentService';
import { DocumentCategory } from '../types/document';
import DocumentViewer from '../components/DocumentViewer';
import FolderExplorer from '../components/FolderExplorer';
import { Folder } from '../services/folderService';
import { ALL_DOCUMENT_TYPES, getDocumentTypeLabel, DocumentType } from '../constants/documentTypes';

const DEFAULT_CATEGORIES: DocumentCategory[] = ALL_DOCUMENT_TYPES.map((type, idx) => ({
  id: -(idx + 1),
  name: type,
  displayName: getDocumentTypeLabel(type),
  description: getDocumentTypeLabel(type),
  isActive: true,
}));

const DocumentsEnhanced: React.FC = () => {
  const { user } = useAppSelector((state) => state.auth);
  
  // State management
  const [documents, setDocuments] = useState<Document[]>([]);
  const [categories, setCategories] = useState<DocumentCategory[]>(DEFAULT_CATEGORIES);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  
  // Pagination
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [pageSize] = useState(10);
  
  // Filters
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('');
  const [filterDepartment, setFilterDepartment] = useState('');
  const [selectedFolderId, setSelectedFolderId] = useState<number | null>(null);
  const [selectedFolder, setSelectedFolder] = useState<Folder | null>(null);
  
  // Upload dialog
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploading, setUploading] = useState(false);
  const [uploadForm, setUploadForm] = useState({
    title: '',
    description: '',
    documentType: DEFAULT_CATEGORIES[0]?.name || DocumentType.OTHER,
    department: user?.department || '',
    tags: '',
    folderId: null as number | null,
  });
  
  // Document viewer
  const [viewerOpen, setViewerOpen] = useState(false);
  const [selectedDocument, setSelectedDocument] = useState<Document | null>(null);
  
  // Menu
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [menuDocument, setMenuDocument] = useState<Document | null>(null);

  // Fetch documents
  const fetchDocuments = useCallback(async () => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await documentService.getDocuments({
        searchTerm: searchTerm || undefined,
        documentType: filterType || undefined,
        department: filterDepartment || undefined,
        folderId: selectedFolderId || undefined,
        page,
        size: pageSize,
      });
      
      setDocuments(response.content || []);
      setTotalPages(response.totalPages || 0);
      setTotalElements(response.totalElements || 0);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch documents');
      console.error('Error fetching documents:', err);
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, searchTerm, filterType, filterDepartment, selectedFolderId]);

  useEffect(() => {
    fetchDocuments();
  }, [fetchDocuments]);

  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const data = await documentService.getDocumentCategories();
        const normalized = (data && data.length > 0) ? data : DEFAULT_CATEGORIES;
        if (!data || data.length === 0) {
          console.warn('[DocumentsEnhanced] No categories returned from API; using defaults');
        }
        setCategories(normalized);
        setUploadForm((prev) => ({
          ...prev,
          documentType: prev.documentType || (normalized[0]?.name ?? ''),
        }));
      } catch (err: any) {
        console.error('Error fetching document categories:', err);
        setCategories(DEFAULT_CATEGORIES);
        setUploadForm((prev) => ({
          ...prev,
          documentType: prev.documentType || DEFAULT_CATEGORIES[0].name,
        }));
        setError(err.response?.data?.message || 'Failed to load document categories; using defaults');
      }
    };

    fetchCategories();
  }, []);

  // Handle file selection
  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      if (!uploadForm.title) {
        setUploadForm({ ...uploadForm, title: file.name });
      }
    }
  };

  // Handle file upload
  const handleUpload = async () => {
    if (!selectedFile || !user?.username) {
      setError('Please select a file and ensure you are logged in');
      return;
    }

    if (!uploadForm.documentType) {
      setError('Please choose a document type');
      return;
    }

    setUploading(true);
    setUploadProgress(0);
    setError(null);

    try {
      // Simulate progress (in real scenario, use axios onUploadProgress)
      const progressInterval = setInterval(() => {
        setUploadProgress((prev) => {
          if (prev >= 90) {
            clearInterval(progressInterval);
            return 90;
          }
          return prev + 10;
        });
      }, 200);

      const uploadRequest: DocumentUploadRequest = {
        file: selectedFile,
        title: uploadForm.title,
        description: uploadForm.description,
        documentType: uploadForm.documentType,
        department: uploadForm.department,
        tags: uploadForm.tags,
        folderId: uploadForm.folderId || selectedFolderId,
        userId: 1, // Should be actual user ID
      };

      await documentService.uploadDocument(uploadRequest);
      
      clearInterval(progressInterval);
      setUploadProgress(100);
      setSuccess('Document uploaded successfully!');
      setUploadDialogOpen(false);
      setSelectedFile(null);
      setUploadForm({
        title: '',
        description: '',
        documentType: categories[0]?.name || DocumentType.OTHER,
        department: user?.department || '',
        tags: '',
        folderId: null,
      });
      
      // Refresh documents list
      fetchDocuments();
      
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to upload document');
      console.error('Upload error:', err);
    } finally {
      setUploading(false);
      setUploadProgress(0);
    }
  };

  // Handle download
  const handleDownload = async (doc: Document) => {
    try {
      const blob = await documentService.downloadDocument(doc.id!);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = doc.fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      setSuccess(`Downloaded: ${doc.fileName}`);
    } catch (err: any) {
      setError(`Failed to download: ${err.message}`);
    }
  };

  // Handle delete
  const handleDelete = async (doc: Document) => {
    if (!window.confirm(`Are you sure you want to delete "${doc.fileName}"?`)) return;
    
    try {
      await documentService.deleteDocument(doc.id!);
      setSuccess('Document deleted successfully');
      fetchDocuments();
    } catch (err: any) {
      setError(`Failed to delete document: ${err.message}`);
    }
  };

  // Handle view
  const handleView = (doc: Document) => {
    setSelectedDocument(doc);
    setViewerOpen(true);
  };

  // Get file icon
  const getFileIcon = (fileName: string) => {
    const ext = fileName.split('.').pop()?.toLowerCase();
    switch (ext) {
      case 'pdf':
        return <PdfIcon sx={{ color: '#f44336' }} />;
      case 'jpg':
      case 'jpeg':
      case 'png':
      case 'gif':
        return <ImageIcon sx={{ color: '#4caf50' }} />;
      case 'doc':
      case 'docx':
        return <DocIcon sx={{ color: '#2196f3' }} />;
      case 'xls':
      case 'xlsx':
        return <ExcelIcon sx={{ color: '#4caf50' }} />;
      default:
        return <FileIcon sx={{ color: '#9e9e9e' }} />;
    }
  };

  // Format file size
  const formatFileSize = (bytes?: string | number) => {
    if (!bytes) return 'N/A';
    const num = typeof bytes === 'string' ? parseInt(bytes) : bytes;
    if (num < 1024) return `${num} B`;
    if (num < 1024 * 1024) return `${(num / 1024).toFixed(1)} KB`;
    return `${(num / (1024 * 1024)).toFixed(1)} MB`;
  };

  // Format date
  const formatDate = (dateString?: string) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

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
            Documents
          </Typography>
          <Typography variant="body2" sx={{ color: '#6b7280' }}>
            Manage and organize your documents ({totalElements} total)
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button
            startIcon={<RefreshIcon />}
            onClick={fetchDocuments}
            variant="outlined"
            sx={{
              textTransform: 'none',
              borderColor: '#d1d5db',
              color: '#374151',
            }}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            startIcon={<UploadIcon />}
            onClick={() => setUploadDialogOpen(true)}
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
            Upload Document
          </Button>
        </Box>
      </Box>

      {/* Alerts */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      {success && (
        <Alert severity="success" sx={{ mb: 3 }} onClose={() => setSuccess(null)}>
          {success}
        </Alert>
      )}

      {/* Folder Explorer */}
      <Card sx={{ mb: 3, borderRadius: 3, boxShadow: '0 1px 3px rgba(0,0,0,0.08)' }}>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6" sx={{ fontWeight: 600 }}>
              Folders
            </Typography>
            {selectedFolder && (
              <Button
                size="small"
                onClick={() => {
                  setSelectedFolderId(null);
                  setSelectedFolder(null);
                }}
                sx={{ textTransform: 'none' }}
              >
                Clear Folder Filter
              </Button>
            )}
          </Box>
          <FolderExplorer
            selectedFolderId={selectedFolderId}
            onFolderSelect={(folder) => {
              setSelectedFolder(folder);
              setSelectedFolderId(folder?.id || null);
              setPage(0);
            }}
          />
        </CardContent>
      </Card>

      {/* Filters */}
      <Card sx={{ mb: 3, borderRadius: 3, boxShadow: '0 1px 3px rgba(0,0,0,0.08)' }}>
        <CardContent>
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                placeholder="Search documents..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon sx={{ color: '#9ca3af' }} />
                    </InputAdornment>
                  ),
                }}
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 2,
                  },
                }}
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <FormControl fullWidth>
                <InputLabel>Document Type</InputLabel>
                <Select
                  value={filterType}
                  label="Document Type"
                  onChange={(e) => setFilterType(e.target.value)}
                  sx={{ borderRadius: 2 }}
                >
                  <MenuItem value="">All Types</MenuItem>
                  {categories.map((category) => (
                    <MenuItem key={category.id} value={category.name}>
                      {category.displayName || category.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={3}>
              <FormControl fullWidth>
                <InputLabel>Department</InputLabel>
                <Select
                  value={filterDepartment}
                  label="Department"
                  onChange={(e) => setFilterDepartment(e.target.value)}
                  sx={{ borderRadius: 2 }}
                >
                  <MenuItem value="">All Departments</MenuItem>
                  <MenuItem value="IT">IT</MenuItem>
                  <MenuItem value="HR">HR</MenuItem>
                  <MenuItem value="Finance">Finance</MenuItem>
                  <MenuItem value="Operations">Operations</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={2}>
              <Button
                fullWidth
                variant="contained"
                onClick={() => {
                  setPage(0);
                  fetchDocuments();
                }}
                sx={{
                  height: '56px',
                  backgroundColor: '#374151',
                  textTransform: 'none',
                  '&:hover': { backgroundColor: '#1f2937' },
                }}
              >
                Apply Filters
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Documents Table */}
      <TableContainer
        component={Paper}
        sx={{
          borderRadius: 3,
          boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
          border: '1px solid #f3f4f6',
        }}
      >
        <Table>
          <TableHead sx={{ backgroundColor: '#f9fafb' }}>
            <TableRow>
              <TableCell sx={{ fontWeight: 600, color: '#374151' }}>File</TableCell>
              <TableCell sx={{ fontWeight: 600, color: '#374151' }}>Type</TableCell>
              <TableCell sx={{ fontWeight: 600, color: '#374151' }}>Department</TableCell>
              <TableCell sx={{ fontWeight: 600, color: '#374151' }}>Size</TableCell>
              <TableCell sx={{ fontWeight: 600, color: '#374151' }}>Uploaded</TableCell>
              <TableCell sx={{ fontWeight: 600, color: '#374151' }}>Status</TableCell>
              <TableCell sx={{ fontWeight: 600, color: '#374151' }} align="right">
                Actions
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ py: 8 }}>
                  <CircularProgress />
                </TableCell>
              </TableRow>
            ) : documents.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ py: 8, color: '#9ca3af' }}>
                  <FileIcon sx={{ fontSize: 48, mb: 2, opacity: 0.5 }} />
                  <Typography>No documents found</Typography>
                </TableCell>
              </TableRow>
            ) : (
              documents.map((doc) => (
                <TableRow key={doc.id} hover>
                  <TableCell>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                      {getFileIcon(doc.fileName)}
                      <Box>
                        <Typography variant="body2" sx={{ fontWeight: 500 }}>
                          {doc.fileName}
                        </Typography>
                        {doc.description && (
                          <Typography variant="caption" sx={{ color: '#6b7280' }}>
                            {doc.description}
                          </Typography>
                        )}
                      </Box>
                    </Box>
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={doc.documentType}
                      size="small"
                      sx={{
                        backgroundColor: '#eff6ff',
                        color: '#1e40af',
                        fontWeight: 500,
                      }}
                    />
                  </TableCell>
                  <TableCell>{doc.department || 'N/A'}</TableCell>
                  <TableCell>{formatFileSize(doc.size)}</TableCell>
                  <TableCell>
                    <Typography variant="body2">{formatDate(doc.createdAt)}</Typography>
                    <Typography variant="caption" sx={{ color: '#6b7280' }}>
                      by {typeof doc.uploadedBy === 'string' ? doc.uploadedBy : doc.uploadedBy?.username || 'Unknown'}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={doc.isActive ? 'Active' : 'Archived'}
                      size="small"
                      color={doc.isActive ? 'success' : 'default'}
                    />
                  </TableCell>
                  <TableCell align="right">
                    <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 0.5 }}>
                      <Tooltip title="View">
                        <IconButton size="small" onClick={() => handleView(doc)}>
                          <ViewIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Download">
                        <IconButton size="small" onClick={() => handleDownload(doc)}>
                          <DownloadIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Delete">
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => handleDelete(doc)}
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </Box>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Pagination */}
      {totalPages > 1 && (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3 }}>
          <Pagination
            count={totalPages}
            page={page + 1}
            onChange={(_, value) => setPage(value - 1)}
            color="primary"
            showFirstButton
            showLastButton
          />
        </Box>
      )}

      {/* Upload Dialog */}
      <Dialog open={uploadDialogOpen} onClose={() => !uploading && setUploadDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Upload Document</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 3 }}>
            <Button
              variant="outlined"
              component="label"
              startIcon={<UploadIcon />}
              fullWidth
              sx={{ py: 2, borderStyle: 'dashed', borderWidth: 2 }}
            >
              {selectedFile ? selectedFile.name : 'Choose File'}
              <input type="file" hidden onChange={handleFileChange} />
            </Button>

            {uploading && (
              <Box>
                <LinearProgress variant="determinate" value={uploadProgress} />
                <Typography variant="caption" sx={{ mt: 1 }}>
                  Uploading... {uploadProgress}%
                </Typography>
              </Box>
            )}

            <TextField
              fullWidth
              label="Title"
              value={uploadForm.title}
              onChange={(e) => setUploadForm({ ...uploadForm, title: e.target.value })}
              required
            />

            <TextField
              fullWidth
              label="Description"
              multiline
              rows={3}
              value={uploadForm.description}
              onChange={(e) => setUploadForm({ ...uploadForm, description: e.target.value })}
            />

            <FormControl fullWidth>
              <InputLabel>Document Type</InputLabel>
              <Select
                value={uploadForm.documentType}
                label="Document Type"
                onChange={(e) => setUploadForm({ ...uploadForm, documentType: e.target.value })}
                disabled={categories.length === 0}
              >
                {categories.map((category) => (
                  <MenuItem key={category.id} value={category.name}>
                    {category.displayName || category.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <TextField
              fullWidth
              label="Department"
              value={uploadForm.department}
              onChange={(e) => setUploadForm({ ...uploadForm, department: e.target.value })}
            />

            <TextField
              fullWidth
              label="Tags (comma-separated)"
              value={uploadForm.tags}
              onChange={(e) => setUploadForm({ ...uploadForm, tags: e.target.value })}
              placeholder="contract, legal, important"
            />

            <FormControl fullWidth>
              <InputLabel>Folder (Optional)</InputLabel>
              <Select
                value={uploadForm.folderId || ''}
                label="Folder (Optional)"
                onChange={(e) => setUploadForm({ ...uploadForm, folderId: e.target.value ? Number(e.target.value) : null })}
              >
                <MenuItem value="">No Folder</MenuItem>
                {selectedFolder && (
                  <MenuItem value={selectedFolder.id}>{selectedFolder.name}</MenuItem>
                )}
              </Select>
              <Typography variant="caption" sx={{ mt: 1, color: 'text.secondary' }}>
                {selectedFolder ? `Current folder: ${selectedFolder.name}` : 'Select a folder from the explorer above'}
              </Typography>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setUploadDialogOpen(false)} disabled={uploading}>
            Cancel
          </Button>
          <Button
            onClick={handleUpload}
            variant="contained"
            disabled={!selectedFile || uploading}
          >
            Upload
          </Button>
        </DialogActions>
      </Dialog>

      {/* Document Viewer */}
      <DocumentViewer
        open={viewerOpen}
        onClose={() => {
          setViewerOpen(false);
          setSelectedDocument(null);
        }}
        document={selectedDocument}
        onDownload={selectedDocument ? () => handleDownload(selectedDocument) : undefined}
      />
    </Box>
  );
};

export default DocumentsEnhanced;

